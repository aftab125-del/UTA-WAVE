package com.uta.wave.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, LRU-cached BitmapLoader for AndroidX Media3 MediaSession.
 *
 * Key features:
 * - Forces allowHardware(false): Android Lockscreen & Notification RemoteViews/IPC
 *   crash with IllegalArgumentException if hardware bitmaps are passed across processes.
 * - 32 MB allocation-aware in-memory LRU cache: Immediate synchronous return for cached covers.
 * - Ahead-of-time queue prefetching: Warms upcoming tracks so the very first metadata
 *   update to Android System UI and Bluetooth / Auto already carries the album artwork.
 */
class CoilBitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope
) : BitmapLoader {

    companion object {
        private const val TARGET_SIZE_PX = 1024
        private const val TIMEOUT_MS = 6000L
    }

    private val bitmapCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val inFlight = ConcurrentHashMap<String, ListenableFuture<Bitmap>>()

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            Futures.immediateFuture(bitmap ?: createFallbackBitmap())
        } catch (e: Exception) {
            Futures.immediateFuture(createFallbackBitmap())
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val key = uri.toString()
        bitmapCache.get(key)?.let {
            return Futures.immediateFuture(it)
        }

        inFlight[key]?.let {
            return it
        }

        val settableFuture = SettableFuture.create<Bitmap>()
        inFlight[key] = settableFuture

        scope.launch(Dispatchers.IO) {
            try {
                val bitmap = fetch(uri)
                bitmapCache.put(key, bitmap)
                settableFuture.set(bitmap)
            } catch (e: Exception) {
                settableFuture.setException(e)
            } finally {
                inFlight.remove(key)
            }
        }

        return settableFuture
    }

    fun prefetch(uri: Uri?) {
        if (uri == null) return
        val key = uri.toString()
        if (bitmapCache.get(key) != null || inFlight.containsKey(key)) return
        loadBitmap(uri)
    }

    private suspend fun fetch(uri: Uri): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(TARGET_SIZE_PX)
            .allowHardware(false) // Mandatory for Notification & Lockscreen IPC
            .build()

        var result = withTimeoutOrNull(TIMEOUT_MS) {
            context.imageLoader.execute(request)
        } ?: throw IOException("Timed out loading artwork from $uri")

        if (result is ErrorResult) {
            val cacheOnlyRequest = request.newBuilder()
                .networkCachePolicy(CachePolicy.READ_ONLY)
                .build()
            result = withTimeoutOrNull(TIMEOUT_MS) {
                context.imageLoader.execute(cacheOnlyRequest)
            } ?: throw IOException("Timed out loading artwork from $uri")
        }

        return when (result) {
            is ErrorResult -> throw result.throwable
            is SuccessResult -> {
                try {
                    val bitmap = result.drawable.toBitmap(
                        width = result.drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(TARGET_SIZE_PX),
                        height = result.drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(TARGET_SIZE_PX),
                        config = Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyIfNeeded()
                } catch (e: Exception) {
                    AppLogger.w("Failed to convert image drawable to software bitmap: ${e.message}")
                    createFallbackBitmap()
                }
            }
        }
    }

    private fun Bitmap.copyIfNeeded(): Bitmap {
        return if (isRecycled) {
            createFallbackBitmap()
        } else {
            try {
                copy(Bitmap.Config.ARGB_8888, false) ?: createFallbackBitmap()
            } catch (e: Exception) {
                createFallbackBitmap()
            }
        }
    }

    private fun createFallbackBitmap(): Bitmap = createBitmap(64, 64, Bitmap.Config.ARGB_8888)
}
