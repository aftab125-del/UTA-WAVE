package com.uta.wave.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object DesktopImageCache {
    private val memoryCache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(url: String): ImageBitmap? = memoryCache[url]
    fun put(url: String, bitmap: ImageBitmap) {
        if (memoryCache.size > 500) {
            memoryCache.clear() // Prevent memory exhaustion on large sessions
        }
        memoryCache[url] = bitmap
    }
}

/**
 * Safely loads raw image bytes from remote URLs (http/https), file:// URIs, or local Windows file paths.
 */
fun loadDesktopImageBytes(urlOrPath: String): ByteArray? {
    return try {
        val trimmed = urlOrPath.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            URI(trimmed).toURL().readBytes()
        } else if (trimmed.startsWith("file://", ignoreCase = true)) {
            File(URI(trimmed)).readBytes()
        } else {
            File(trimmed).readBytes()
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Suspending safe loader that queries memory cache, loads from IO, decodes Skia ImageBitmap, and stores in cache.
 */
suspend fun loadDesktopImageBitmapSafe(urlOrPath: String?): ImageBitmap? {
    if (urlOrPath.isNullOrBlank()) return null
    val cached = DesktopImageCache.get(urlOrPath)
    if (cached != null) return cached
    return withContext(Dispatchers.IO) {
        try {
            val bytes = loadDesktopImageBytes(urlOrPath) ?: return@withContext null
            val loaded = loadImageBitmap(ByteArrayInputStream(bytes))
            DesktopImageCache.put(urlOrPath, loaded)
            loaded
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Reusable high-performance image thumbnail loader for Windows Desktop.
 * Utilizes an in-memory cache to prevent flickering and network churn during scrolling.
 */
@Composable
fun DesktopNetworkImage(
    url: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = Color(0xFF1E1728)
) {
    var bitmap by remember(url) { mutableStateOf(DesktopImageCache.get(url)) }

    LaunchedEffect(url) {
        if (bitmap == null && url.isNotBlank()) {
            bitmap = loadDesktopImageBitmapSafe(url)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(placeholderColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
