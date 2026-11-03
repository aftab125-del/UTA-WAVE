package com.uta.wave.desktop.audio

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Embedded local HTTP audio proxy running on localhost.
 *
 * Solves 3 critical Windows media streaming requirements:
 * 1. Attaches proper User-Agent & Referer required by YouTube / Google Video CDN.
 * 2. Enforces Range requests (bytes=0-) to prevent Google Video's 30 kB/s throttling.
 * 3. Delivers 206 Partial Content over 0ms localhost to Windows Media Foundation.
 */
class LocalAudioProxy(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    private var server: HttpServer? = null
    var port: Int = 0
        private set

    fun start() {
        if (server != null) return

        try {
            // Bind to ephemeral port on loopback interface
            val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            http.executor = Executors.newFixedThreadPool(4)
            http.createContext("/stream.m4a") { exchange -> handleAudioRequest(exchange) }
            http.createContext("/audio") { exchange -> handleAudioRequest(exchange) }
            http.start()
            server = http
            port = http.address.port
            println("[LocalAudioProxy] Started on http://127.0.0.1:$port/stream.m4a")
        } catch (e: Exception) {
            println("[LocalAudioProxy] Failed to bind local audio proxy: ${e.message}")
        }
    }

    fun stop() {
        try {
            server?.stop(0)
            server = null
        } catch (e: Exception) {
            println("[LocalAudioProxy] Error stopping proxy: ${e.message}")
        }
    }

    /**
     * Converts a raw upstream streaming URL into a localhost proxy URL with .m4a extension.
     */
    fun getProxyUrl(upstreamUrl: String, clientPersona: String = "VISIONOS"): String {
        if (port == 0) start()
        val encodedUrl = URLEncoder.encode(upstreamUrl, StandardCharsets.UTF_8.toString())
        return "http://127.0.0.1:$port/stream.m4a?url=$encodedUrl&client=$clientPersona"
    }

    private fun handleAudioRequest(exchange: HttpExchange) {
        val method = exchange.requestMethod
        val uri = exchange.requestURI
        println("[LocalAudioProxy] ---> $method $uri")
        println("[LocalAudioProxy] Headers: ${exchange.requestHeaders.entries.joinToString { "${it.key}: ${it.value}" }}")
        val isHead = method.equals("HEAD", ignoreCase = true)
        val query = exchange.requestURI.rawQuery ?: ""
        var targetUrl: String? = null
        var clientPersona = "VISIONOS"

        for (param in query.split("&")) {
            val parts = param.split("=")
            if (parts.size >= 2) {
                val key = parts[0]
                val value = URLDecoder.decode(parts.subList(1, parts.size).joinToString("="), StandardCharsets.UTF_8.toString())
                if (key == "url") targetUrl = value
                if (key == "client") clientPersona = value
            }
        }

        if (targetUrl.isNullOrBlank()) {
            exchange.sendResponseHeaders(400, -1)
            exchange.close()
            return
        }

        val clientRange = exchange.requestHeaders.getFirst("Range")
        val upstreamRange = clientRange ?: "bytes=0-"
        val userAgent = when (clientPersona.uppercase()) {
            "VISIONOS" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"
            "ANDROID_VR" -> "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
            "IOS" -> "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1 like Mac OS X; en_US)"
            else -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        }

        val okReqBuilder = Request.Builder()
            .url(targetUrl)
            .addHeader("User-Agent", userAgent)
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Range", upstreamRange)

        if (isHead) {
            okReqBuilder.head()
        }

        try {
            val okRes = httpClient.newCall(okReqBuilder.build()).execute()
            val upstreamCode = okRes.code
            val body = okRes.body

            // Extract total size from Content-Range (e.g. bytes 0-5100875/5100876) or Content-Length
            val contentRange = okRes.header("Content-Range")
            val totalLength = if (contentRange != null && contentRange.contains("/")) {
                contentRange.substringAfter("/").toLongOrNull() ?: body?.contentLength() ?: -1L
            } else {
                body?.contentLength() ?: -1L
            }

            // If the local client did NOT request a Range, we must respond with 200 OK (not 206)
            val clientCode = if (clientRange == null && upstreamCode == 206) 200 else upstreamCode

            // Forward response headers
            val responseHeaders = exchange.responseHeaders
            responseHeaders.set("Content-Type", okRes.header("Content-Type") ?: "audio/mp4")
            responseHeaders.set("Accept-Ranges", "bytes")
            if (clientRange != null && contentRange != null) {
                responseHeaders.set("Content-Range", contentRange)
            }
            if (totalLength > 0 && clientCode == 200) {
                responseHeaders.set("Content-Length", totalLength.toString())
            } else {
                okRes.header("Content-Length")?.let { responseHeaders.set("Content-Length", it) }
            }
            responseHeaders.set("Access-Control-Allow-Origin", "*")

            if (isHead) {
                println("[LocalAudioProxy] Sending HEAD response: code=$clientCode, totalLength=$totalLength")
                exchange.sendResponseHeaders(clientCode, -1)
                exchange.close()
                okRes.close()
                return
            }

            val bodyLen = if (clientCode == 200 && totalLength > 0) totalLength else (body?.contentLength() ?: 0L)
            println("[LocalAudioProxy] Sending GET response: code=$clientCode, clientRange=$clientRange, bodyLen=$bodyLen")
            exchange.sendResponseHeaders(clientCode, if (bodyLen > 0) bodyLen else 0)

            body?.byteStream()?.use { input: InputStream ->
                exchange.responseBody.use { output: OutputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
        } catch (e: Exception) {
            try {
                exchange.sendResponseHeaders(502, -1)
                exchange.close()
            } catch (_: Exception) {}
        }
    }
}
