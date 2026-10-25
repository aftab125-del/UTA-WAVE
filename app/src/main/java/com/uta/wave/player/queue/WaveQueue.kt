package com.uta.wave.player.queue

import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Polymorphic Queue interface for UTA-WAVE.
 * Supports static lists, infinite algorithmic radios, and dynamic continuation paging.
 */
interface WaveQueue {
    val title: String?
    suspend fun getInitialTracks(): List<TrackItem>
    fun hasNextPage(): Boolean
    suspend fun nextPage(): List<TrackItem>
}

/**
 * Loop prevention filter for radio queues.
 * Drops tracks that were already seen across all pagination waves.
 */
class RadioQueuePageFilter {
    private val seenTrackIds = mutableSetOf<String>()

    fun reset() {
        seenTrackIds.clear()
    }

    fun markSeen(trackId: String) {
        val clean = trackId.removePrefix("youtube:")
        seenTrackIds.add(clean)
        seenTrackIds.add(trackId)
    }

    fun filter(tracks: List<TrackItem>): List<TrackItem> {
        return tracks.filter { track ->
            val clean = track.videoId.ifBlank { track.id.removePrefix("youtube:") }
            if (seenTrackIds.contains(clean) || seenTrackIds.contains(track.id)) {
                false
            } else {
                seenTrackIds.add(clean)
                seenTrackIds.add(track.id)
                true
            }
        }
    }
}

/**
 * YouTube Music Algorithmic Radio Queue.
 * Expands any single track or radio endpoint (RDAMVM<videoId>) into an infinite continuous stream.
 */
class YouTubeRadioQueue(
    private val videoId: String,
    private val initialTrack: TrackItem? = null,
    private val innerTubeClient: InnerTubeClient = InnerTubeClient()
) : WaveQueue {

    private var continuation: String? = null
    private val pageFilter = RadioQueuePageFilter()
    private val maxRetries = 3

    override val title: String
        get() = initialTrack?.let { "${it.title} Radio" } ?: "Algorithmic Radio"

    override suspend fun getInitialTracks(): List<TrackItem> = withContext(Dispatchers.IO) {
        continuation = null
        pageFilter.reset()

        initialTrack?.let { pageFilter.markSeen(it.videoId) }

        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                val nextResult = innerTubeClient.getNext(
                    videoId = videoId,
                    playlistId = "RDAMVM$videoId",
                    continuation = null
                )

                if (nextResult != null && nextResult.items.isNotEmpty()) {
                    continuation = nextResult.continuation

                    val filtered = pageFilter.filter(nextResult.items)
                    val resultList = mutableListOf<TrackItem>()

                    if (initialTrack != null) {
                        resultList.add(initialTrack)
                        // Add the rest avoiding immediate duplicate of initialTrack
                        for (item in filtered) {
                            if (item.videoId != initialTrack.videoId) {
                                resultList.add(item)
                            }
                        }
                    } else {
                        resultList.addAll(filtered)
                    }

                    AppLogger.i("YouTubeRadioQueue initial tracks loaded: ${resultList.size}, hasContinuation=${continuation != null}")
                    return@withContext resultList
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastException = e
                AppLogger.w("YouTubeRadioQueue attempt $attempt failed: ${e.message}")
            }
        }

        // Fallback: If network fails, return the initial track if available
        if (initialTrack != null) {
            return@withContext listOf(initialTrack)
        }

        throw lastException ?: Exception("Failed to load initial radio station for $videoId")
    }

    override fun hasNextPage(): Boolean = continuation != null

    override suspend fun nextPage(): List<TrackItem> = withContext(Dispatchers.IO) {
        val visitedContinuations = mutableSetOf<String>()

        repeat(5) {
            val pageContinuation = continuation ?: return@withContext emptyList()
            if (!visitedContinuations.add(pageContinuation)) {
                continuation = null
                return@withContext emptyList()
            }

            for (attempt in 0..maxRetries) {
                try {
                    val nextResult = innerTubeClient.getNext(continuation = pageContinuation)
                    if (nextResult != null) {
                        continuation = nextResult.continuation?.takeUnless { it == pageContinuation }
                        val songs = pageFilter.filter(nextResult.items)
                        if (songs.isNotEmpty()) {
                            AppLogger.i("YouTubeRadioQueue paged ${songs.size} new tracks (nextContinuation=${continuation != null})")
                            return@withContext songs
                        }
                        break
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    AppLogger.w("YouTubeRadioQueue nextPage attempt $attempt failed: ${e.message}")
                    if (attempt == maxRetries) {
                        continuation = null
                        throw e
                    }
                }
            }
        }

        emptyList()
    }
}

/**
 * Fixed list queue (e.g. playing an album, local playlist, search result batch).
 */
class ListQueue(
    private val tracks: List<TrackItem>,
    override val title: String? = "Queue"
) : WaveQueue {
    override suspend fun getInitialTracks(): List<TrackItem> = tracks
    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<TrackItem> = emptyList()
}

/**
 * Occurrence-aware track filtering.
 * Recalculates index based on exact occurrence so duplicate songs don't disrupt current playback.
 */
object OccurrenceAwareFilter {
    fun filterTracks(
        items: List<TrackItem>,
        currentIndex: Int,
        predicate: (TrackItem) -> Boolean
    ): Pair<List<TrackItem>, Int> {
        val currentTrack = items.getOrNull(currentIndex)
        val filtered = items.filter(predicate)

        if (currentTrack == null || filtered.isEmpty()) {
            return Pair(filtered, currentIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0)))
        }

        // Count how many times currentTrack appeared up to currentIndex
        val occurrence = items.take(currentIndex + 1).count { it.id == currentTrack.id } - 1

        val newIndex = filtered.withIndex()
            .filter { (_, item) -> item.id == currentTrack.id }
            .getOrNull(occurrence)
            ?.index
            ?: filtered.indexOfFirst { it.id == currentTrack.id }.takeIf { it >= 0 }
            ?: currentIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))

        return Pair(filtered, newIndex)
    }
}
