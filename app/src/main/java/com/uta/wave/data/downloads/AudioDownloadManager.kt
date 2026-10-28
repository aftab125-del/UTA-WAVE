package com.uta.wave.data.downloads

import android.content.Context
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.data.innertube.PlayerClient
import com.uta.wave.data.innertube.TrackItem
import com.uta.wave.data.local.LocalDatabase
import com.uta.wave.data.lyrics.LyricsClient
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class AudioQuality(val label: String, val badge: String, val description: String) {
    LOSSLESS("Lossless / Master", "LOSSLESS", "256 kbps AAC / 160 kbps Opus • Studio Fidelity"),
    HIGH("High Quality", "HIGH", "160 kbps • Crystal clear audio"),
    STANDARD("Standard Quality", "STANDARD", "128 kbps M4A • Compact size")
}

data class BatchDownloadProgress(
    val isDownloading: Boolean = false,
    val currentTrackIndex: Int = 0,
    val totalTracks: Int = 0,
    val currentTrackTitle: String = "",
    val currentTrackProgress: Float = 0f
)

class AudioDownloadManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AudioDownloadManager? = null

        fun getInstance(context: Context): AudioDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val localDatabase = LocalDatabase.getInstance(context)
    private val innerTubeClient = InnerTubeClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _batchProgress = MutableStateFlow(BatchDownloadProgress())
    val batchProgress: StateFlow<BatchDownloadProgress> = _batchProgress.asStateFlow()

    private var batchJob: Job? = null

    private val downloadsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "downloads")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun isDownloaded(trackId: String): Boolean {
        return localDatabase.isDownloadedSync(trackId)
    }

    fun getDownloadFile(trackId: String): File? {
        val path = localDatabase.getDownloadPathSync(trackId) ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 0) file else null
    }

    suspend fun downloadTrackSuspend(
        track: TrackItem,
        quality: AudioQuality = AudioQuality.LOSSLESS,
        trackIndex: Int = 1,
        totalCount: Int = 1
    ): Boolean = withContext(Dispatchers.IO) {
        if (isDownloaded(track.id) || isDownloaded(track.videoId)) {
            AppLogger.i("Track already downloaded: ${track.title}")
            return@withContext true
        }

        _downloadingIds.update { it + track.id }
        _downloadProgress.update { it + (track.id to 0f) }

        try {
            AppLogger.i("Starting lossless download for: ${track.title} (${quality.badge})")
            val streamUrl = innerTubeClient.resolveStreamUrl(track.videoId, quality.badge)
            if (streamUrl.isNullOrBlank()) {
                AppLogger.e("Failed to resolve lossless stream URL for download: ${track.title}")
                _downloadingIds.update { it - track.id }
                _downloadProgress.update { it - track.id }
                return@withContext false
            }

            val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val tempFile = File(downloadsDir, "$safeId.tmp")
            val outputFile = File(downloadsDir, "$safeId.m4a")

            // Dress the request with exact matching client User-Agent and headers
            // (fixes HTTP 403 Forbidden on Google Video CDN, identical to EchoMusic PlayerClient)
            val playerClient = PlayerClient.forStreamUrl(streamUrl)
            val mediaHeaders = playerClient.mediaHeaders()

            val requestBuilder = Request.Builder()
                .url(streamUrl)
                .header("Range", "bytes=0-")

            for ((headerKey, headerVal) in mediaHeaders) {
                requestBuilder.header(headerKey, headerVal)
            }

            val request = requestBuilder.build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    AppLogger.e("Download HTTP failed (${playerClient.clientName}): ${response.code} ${response.message}")
                    if (tempFile.exists()) tempFile.delete()
                    _downloadingIds.update { it - track.id }
                    _downloadProgress.update { it - track.id }
                    return@withContext false
                }

                val body = response.body ?: run {
                    if (tempFile.exists()) tempFile.delete()
                    _downloadingIds.update { it - track.id }
                    _downloadProgress.update { it - track.id }
                    return@withContext false
                }

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(32768) // 32KB buffer for optimal I/O throughput
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdateMs = 0L

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            _downloadProgress.update { it + (track.id to progress) }
                            _batchProgress.update { it.copy(currentTrackProgress = progress) }

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdateMs > 750) {
                                lastProgressUpdateMs = now
                                AudioDownloadService.updateProgress(
                                    context = context,
                                    title = track.title,
                                    progressPercent = (progress * 100).toInt(),
                                    index = trackIndex,
                                    total = totalCount
                                )
                            }
                        }
                    }
                    outputStream.flush()
                } finally {
                    try { outputStream.close() } catch (e: Exception) { /* ignore */ }
                    try { inputStream.close() } catch (e: Exception) { /* ignore */ }
                }

                if (tempFile.exists() && tempFile.length() > 0L) {
                    if (outputFile.exists()) outputFile.delete()
                    val renamed = tempFile.renameTo(outputFile)
                    val finalFile = if (renamed) outputFile else tempFile

                    localDatabase.insertDownload(track, finalFile.absolutePath, finalFile.length(), quality.badge)
                    AppLogger.i("Lossless download complete (${quality.badge}) for: ${track.title} (${finalFile.length() / 1024} KB)")

                    // Pre-cache artwork for offline viewing in Airplane Mode
                    if (track.artworkUrl.isNotBlank()) {
                        try {
                            val imageRequest = ImageRequest.Builder(context)
                                .data(track.artworkUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .build()
                            Coil.imageLoader(context).enqueue(imageRequest)
                        } catch (e: Exception) {
                            AppLogger.w("Artwork pre-cache failed for ${track.title}: ${e.message}")
                        }
                    }

                    // Pre-fetch lyrics in background for offline synced lyrics
                    scope.launch(Dispatchers.IO) {
                        try {
                            LyricsClient().getLyrics(track.title, track.artist, track.durationSeconds)
                        } catch (e: Exception) { /* ignore */ }
                    }

                    _downloadingIds.update { it - track.id }
                    _downloadProgress.update { it - track.id }
                    return@withContext true
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    AppLogger.e("Downloaded temp file is empty or missing: ${tempFile.absolutePath}")
                    _downloadingIds.update { it - track.id }
                    _downloadProgress.update { it - track.id }
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Download error for: ${track.title}: ${e.message}", e)
            try {
                val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val tempFile = File(downloadsDir, "$safeId.tmp")
                if (tempFile.exists()) tempFile.delete()
            } catch (ex: Exception) { /* ignore */ }
            _downloadingIds.update { it - track.id }
            _downloadProgress.update { it - track.id }
            return@withContext false
        }
    }

    fun downloadTrack(
        track: TrackItem,
        quality: AudioQuality = AudioQuality.LOSSLESS,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (_downloadingIds.value.contains(track.id) || isDownloaded(track.id) || isDownloaded(track.videoId)) {
            onComplete(true)
            return
        }

        AudioDownloadService.start(context, track.title, track.artist)

        scope.launch {
            val success = downloadTrackSuspend(track, quality, trackIndex = 1, totalCount = 1)
            if (_downloadingIds.value.isEmpty() && !_batchProgress.value.isDownloading) {
                AudioDownloadService.stop(context)
            }
            withContext(Dispatchers.Main) { onComplete(success) }
        }
    }

    fun deleteDownload(trackId: String) {
        scope.launch {
            val path = localDatabase.getDownloadPathSync(trackId)
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) file.delete()
            }
            try {
                val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val tempFile = File(downloadsDir, "$safeId.tmp")
                if (tempFile.exists()) tempFile.delete()
            } catch (e: Exception) { /* ignore */ }

            localDatabase.deleteDownload(trackId)
            AppLogger.i("Deleted download for trackId: $trackId")
        }
    }

    /**
     * Downloads playlist songs sequentially (one-by-one) in Lossless quality,
     * maintaining device memory and preventing mobile data network throttling.
     * Keeps foreground download notification active until complete.
     */
    fun downloadTracks(
        tracks: List<TrackItem>,
        quality: AudioQuality = AudioQuality.LOSSLESS
    ) {
        batchJob?.cancel()
        batchJob = scope.launch {
            val pending = tracks.filter { !isDownloaded(it.id) && !isDownloaded(it.videoId) }
            if (pending.isEmpty()) {
                _batchProgress.value = BatchDownloadProgress(isDownloading = false)
                AudioDownloadService.stop(context)
                return@launch
            }

            AudioDownloadService.start(context, pending.first().title, pending.first().artist)

            _batchProgress.value = BatchDownloadProgress(
                isDownloading = true,
                currentTrackIndex = 0,
                totalTracks = pending.size,
                currentTrackTitle = pending.first().title,
                currentTrackProgress = 0f
            )

            var successCount = 0
            var failCount = 0

            for (index in pending.indices) {
                val track = pending[index]
                _batchProgress.value = BatchDownloadProgress(
                    isDownloading = true,
                    currentTrackIndex = index + 1,
                    totalTracks = pending.size,
                    currentTrackTitle = track.title,
                    currentTrackProgress = 0f
                )

                AudioDownloadService.updateProgress(
                    context = context,
                    title = track.title,
                    progressPercent = 0,
                    index = index + 1,
                    total = pending.size
                )

                AppLogger.i("Processing playlist track ${index + 1}/${pending.size} (Lossless): ${track.title}")
                val success = downloadTrackSuspend(track, quality, trackIndex = index + 1, totalCount = pending.size)
                if (success) {
                    successCount++
                } else {
                    failCount++
                }

                // Small 250ms spacing to keep network socket and storage healthy
                delay(250)
            }

            _batchProgress.value = BatchDownloadProgress(isDownloading = false)
            AudioDownloadService.stop(context)
            AppLogger.i("Sequential playlist download finished: $successCount downloaded, $failCount failed")
        }
    }

    fun cancelBatchDownload() {
        batchJob?.cancel()
        _batchProgress.value = BatchDownloadProgress(isDownloading = false)
        AudioDownloadService.stop(context)
    }

    fun deleteTracks(tracks: List<TrackItem>) {
        cancelBatchDownload()
        tracks.forEach { track ->
            deleteDownload(track.id)
            deleteDownload(track.videoId)
        }
    }

    fun areAllTracksDownloaded(tracks: List<TrackItem>): Boolean {
        if (tracks.isEmpty()) return false
        return tracks.all { isDownloaded(it.id) || isDownloaded(it.videoId) }
    }

    fun isAnyTrackDownloading(tracks: List<TrackItem>): Boolean {
        val downloading = _downloadingIds.value
        return tracks.any { downloading.contains(it.id) || downloading.contains(it.videoId) } || _batchProgress.value.isDownloading
    }

    fun countDownloadedTracks(tracks: List<TrackItem>): Int {
        return tracks.count { isDownloaded(it.id) || isDownloaded(it.videoId) }
    }
}
