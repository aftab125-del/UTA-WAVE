package com.uta.wave.desktop.audio

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Embedded local HTTP audio proxy running on pure java.net.ServerSocket (standard java.base).
 *
 * Has zero external module dependencies (no jdk.httpserver required).
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
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newFixedThreadPool(6)
    var port: Int = 0
        private set

    fun start() {
        if (serverSocket != null && !serverSocket!!.isClosed) return

        try {
            val ss = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = ss
            port = ss.localPort
            println("[LocalAudioProxy] Started on http://127.0.0.1:$port/stream.m4a (pure ServerSocket)")

            thread(isDaemon = true, name = "LocalAudioProxy-Acceptor") {
                while (!ss.isClosed) {
                    try {
                        val client = ss.accept()
                        executor.submit { handleClient(client) }
                    } catch (e: Exception) {
                        if (ss.isClosed) break
                    }
                }
            }
        } catch (e: Throwable) {
            println("[LocalAudioProxy] Failed to bind local audio proxy: ${e.message}")
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            executor.shutdownNow()
        } catch (e: Exception) {
            println("[LocalAudioProxy] Error stopping proxy: ${e.message}")
        }
    }

    fun getProxyUrl(upstreamUrl: String, clientPersona: String = "VISIONOS"): String {
        if (port == 0) start()
        val encodedUrl = URLEncoder.encode(upstreamUrl, StandardCharsets.UTF_8.toString())
        return "http://127.0.0.1:$port/stream.m4a?url=$encodedUrl&client=$clientPersona"
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val fullPath = parts[1]
                val isHead = method.equals("HEAD", ignoreCase = true)

                // Read HTTP request headers
                val headers = mutableMapOf<String, String>()
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrEmpty()) {
                    val colonIdx = headerLine.indexOf(':')
                    if (colonIdx > 0) {
                        val key = headerLine.substring(0, colonIdx).trim().lowercase()
                        val value = headerLine.substring(colonIdx + 1).trim()
                        headers[key] = value
                    }
                    headerLine = reader.readLine()
                }

                // Parse query parameters
                val queryIdx = fullPath.indexOf('?')
                val query = if (queryIdx >= 0) fullPath.substring(queryIdx + 1) else ""
                var targetUrl: String? = null
                var clientPersona = "VISIONOS"

                for (param in query.split("&")) {
                    val pParts = param.split("=")
                    if (pParts.size >= 2) {
                        val key = pParts[0]
                        val value = URLDecoder.decode(pParts.subList(1, pParts.size).joinToString("="), StandardCharsets.UTF_8.toString())
                        if (key == "url") targetUrl = value
                        if (key == "client") clientPersona = value
                    }
                }

                val out = client.getOutputStream()

                if (targetUrl.isNullOrBlank()) {
                    writeHttpResponse(out, 400, "Bad Request", mapOf("Content-Length" to "0"), isHead = true)
                    return
                }

                val clientRange = headers["range"]
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

                if (isHead) okReqBuilder.head()

                val okRes = httpClient.newCall(okReqBuilder.build()).execute()
                val upstreamCode = okRes.code
                val body = okRes.body

                val contentRange = okRes.header("Content-Range")
                val totalLength = if (contentRange != null && contentRange.contains("/")) {
                    contentRange.substringAfter("/").toLongOrNull() ?: body?.contentLength() ?: -1L
                } else {
                    body?.contentLength() ?: -1L
                }

                val clientCode = if (clientRange == null && upstreamCode == 206) 200 else upstreamCode
                val responseHeaders = mutableMapOf<String, String>()
                responseHeaders["Content-Type"] = okRes.header("Content-Type") ?: "audio/mp4"
                responseHeaders["Accept-Ranges"] = "bytes"
                responseHeaders["Access-Control-Allow-Origin"] = "*"

                if (clientRange != null && contentRange != null) {
                    responseHeaders["Content-Range"] = contentRange
                }

                if (totalLength > 0 && clientCode == 200) {
                    responseHeaders["Content-Length"] = totalLength.toString()
                } else {
                    okRes.header("Content-Length")?.let { responseHeaders["Content-Length"] = it }
                }

                val statusMessage = when (clientCode) {
                    200 -> "OK"
                    206 -> "Partial Content"
                    404 -> "Not Found"
                    else -> "Response $clientCode"
                }

                writeHttpResponse(out, clientCode, statusMessage, responseHeaders, isHead = isHead)

                if (!isHead && body != null) {
                    body.byteStream().use { input: InputStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                        out.flush()
                    }
                }
                okRes.close()
            }
        } catch (e: Exception) {
            // Socket disconnected or stream finished normally
        }
    }

    private fun writeHttpResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        headers: Map<String, String>,
        isHead: Boolean
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n")
        for ((k, v) in headers) {
            sb.append(k).append(": ").append(v).append("\r\n")
        }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(StandardCharsets.ISO_8859_1))
        out.flush()
    }
}
