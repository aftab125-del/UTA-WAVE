package com.uta.wave.data.innertube

import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/**
 * The identity a googlevideo URL says minted it, so the media fetch repeats it accurately.
 *
 * googlevideo bakes the client into the URL as `c=`/`cver=` and compares it against the headers
 * of the request fetching the bytes. A mismatch (e.g. sending desktop Chrome UA to an ANDROID_VR URL)
 * causes Google Video CDN to respond with HTTP 403 Forbidden or throttle playback to a crawl.
 *
 * Modeled after EchoMusic's proven PlayerClient architecture.
 */
data class PlayerClient(
    val clientName: String,
    val clientVersion: String,
    val userAgent: String,
    val origin: String? = null
) {
    val referer: String?
        get() = origin?.let { "$it/" }

    /**
     * Headers the media request must carry for a URL minted with this client.
     */
    fun mediaHeaders(): Map<String, String> = buildMap {
        put("User-Agent", userAgent)
        origin?.let { put("Origin", it) }
        referer?.let { put("Referer", it) }
        put("Accept", "*/*")
        put("Accept-Encoding", "identity")
    }

    companion object {
        private const val MUSIC_ORIGIN = "https://music.youtube.com"
        private const val YOUTUBE_ORIGIN = "https://www.youtube.com"

        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

        private val IOS = PlayerClient(
            clientName = "IOS",
            clientVersion = "21.26.4",
            userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
        )

        private val IOS_RECENT = IOS.copy(
            clientVersion = "21.29.1",
            userAgent = "com.google.ios.youtube/21.29.1 (iPhone16,2; U; CPU iOS 18_5 like Mac OS X;)"
        )

        private val ANDROID = PlayerClient(
            clientName = "ANDROID",
            clientVersion = "21.26.364",
            userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip"
        )

        private val ANDROID_MUSIC = PlayerClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "8.39.42",
            userAgent = "com.google.android.apps.youtube.music/8.39.42 (Linux; U; Android 15; en_US; Pixel 9 Pro; Build/AP4A.250205.002) gzip"
        )

        private val ANDROID_VR = PlayerClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
        )

        private val ANDROID_VR_LEGACY = ANDROID_VR.copy(
            clientVersion = "1.43.32",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)"
        )

        private val WEB_REMIX = PlayerClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260707.12.00",
            userAgent = WEB_USER_AGENT,
            origin = MUSIC_ORIGIN
        )

        private val WEB = PlayerClient(
            clientName = "WEB",
            clientVersion = "2.20260708.00.00",
            userAgent = WEB_USER_AGENT,
            origin = YOUTUBE_ORIGIN
        )

        private val TVHTML5 = PlayerClient(
            clientName = "TVHTML5",
            clientVersion = "7.20260707.07.00",
            userAgent = "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15",
            origin = YOUTUBE_ORIGIN
        )

        private fun visionOs(version: String?) = PlayerClient(
            clientName = "VISIONOS",
            clientVersion = version ?: "1.02",
            userAgent = "com.google.ios.youtube/1.02 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/605.1.15 (KHTML, like Gecko)"
        )

        /**
         * Resolves the PlayerClient that matches the `c` query parameter in the googlevideo URL.
         */
        fun forStreamUrl(url: String): PlayerClient {
            val parsed = url.toHttpUrlOrNull()
            val name = (parsed?.queryParameter("c") ?: try {
                Uri.parse(url).getQueryParameter("c")
            } catch (e: Exception) {
                null
            })?.uppercase(Locale.ROOT) ?: return IOS

            val version = parsed?.queryParameter("cver") ?: try {
                Uri.parse(url).getQueryParameter("cver")
            } catch (e: Exception) {
                null
            }

            return when {
                name.startsWith("IOS") -> if (version == IOS_RECENT.clientVersion) IOS_RECENT else IOS
                name == "ANDROID_VR" -> if (version == ANDROID_VR_LEGACY.clientVersion) ANDROID_VR_LEGACY else ANDROID_VR
                name == "ANDROID_MUSIC" -> ANDROID_MUSIC
                name.startsWith("ANDROID") -> ANDROID
                name.startsWith("TVHTML5") -> TVHTML5
                name == "WEB_REMIX" -> WEB_REMIX
                name.startsWith("WEB") || name == "MWEB" -> WEB
                name == "VISIONOS" -> visionOs(version)
                else -> IOS
            }
        }
    }
}
