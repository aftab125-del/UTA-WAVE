package com.uta.wave.desktop.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class DesktopUserProfile(
    val isLoggedIn: Boolean = true,
    val userId: String = "d3be4e1b-af5d-4dc4-80ae-8fb0e3ca969c",
    val name: String = "Samurai Gaming",
    val email: String = "Google Account Connected",
    val avatarUrl: String? = "https://lh3.googleusercontent.com/a/ACg8ocKRYAmjD5cwzakrVPq_EeNxAdlQDsOQ-O1W_glUZHAT_s7UqVGVWQ=s96-c",
    val handle: String? = "@samurai",
    val cookie: String? = null,
    val sapisid: String? = null
)

class DesktopAccountManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: DesktopAccountManager? = null

        fun getInstance(): DesktopAccountManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DesktopAccountManager().also { INSTANCE = it }
            }
        }

        fun computeSapisidHash(sapisid: String): String {
            val timestamp = System.currentTimeMillis() / 1000
            val input = "$timestamp $sapisid https://music.youtube.com"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            val sha1 = digest.joinToString("") { "%02x".format(it) }
            return "${timestamp}_$sha1"
        }

        private fun getSessionFile(): File {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
            val dir = File(localAppData, "UTA-wave")
            if (!dir.exists()) dir.mkdirs()
            return File(dir, "session.json")
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessionFile = getSessionFile()

    private val _profile = MutableStateFlow(loadSession())
    val profile: StateFlow<DesktopUserProfile> = _profile.asStateFlow()

    val currentProfile: DesktopUserProfile
        get() = _profile.value

    private fun loadSession(): DesktopUserProfile {
        try {
            if (!sessionFile.exists()) {
                val defaultProf = DesktopUserProfile()
                try {
                    val json = JSONObject().apply {
                        put("is_logged_in", true)
                        put("user_id", defaultProf.userId)
                        put("name", defaultProf.name)
                        put("email", defaultProf.email)
                        put("avatar_url", defaultProf.avatarUrl ?: "")
                        put("handle", defaultProf.handle ?: "")
                        put("saved_at", System.currentTimeMillis())
                    }
                    sessionFile.writeText(json.toString(2), Charsets.UTF_8)
                } catch (_: Exception) {}
                return defaultProf
            }
            val content = sessionFile.readText(Charsets.UTF_8)
            if (content.isBlank()) return DesktopUserProfile()

            val json = JSONObject(content)
            val isLoggedIn = json.optBoolean("is_logged_in", true)
            val userId = json.optString("user_id", "d3be4e1b-af5d-4dc4-80ae-8fb0e3ca969c")
            val name = json.optString("name", "Samurai Gaming")
            val email = json.optString("email", "Google Account Connected")
            val avatarUrl = json.optString("avatar_url").takeIf { !it.isNullOrBlank() }
                ?: "https://lh3.googleusercontent.com/a/ACg8ocKRYAmjD5cwzakrVPq_EeNxAdlQDsOQ-O1W_glUZHAT_s7UqVGVWQ=s96-c"
            val handle = json.optString("handle").takeIf { !it.isNullOrBlank() } ?: "@samurai"
            val cookie = json.optString("cookie").takeIf { !it.isNullOrBlank() }
            val sapisid = json.optString("sapisid").takeIf { !it.isNullOrBlank() }
                ?: cookie?.let { extractCookiePart(it, "SAPISID") ?: extractCookiePart(it, "__Secure-3PAPISID") }

            return DesktopUserProfile(
                isLoggedIn = isLoggedIn,
                userId = userId,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                handle = handle,
                cookie = cookie,
                sapisid = sapisid
            )
        } catch (e: Exception) {
            println("[DesktopAccountManager] Failed to load session: ${e.message}")
            return DesktopUserProfile()
        }
    }

    fun saveSession(
        cookieHeader: String,
        userId: String = "d3be4e1b-af5d-4dc4-80ae-8fb0e3ca969c",
        userName: String = "Samurai Gaming",
        userEmail: String = "Google Account Connected",
        avatarUrl: String? = null,
        handle: String? = null
    ) {
        val sapisid = extractCookiePart(cookieHeader, "SAPISID")
            ?: extractCookiePart(cookieHeader, "__Secure-3PAPISID")

        val newProfile = DesktopUserProfile(
            isLoggedIn = true,
            userId = userId,
            name = userName,
            email = userEmail,
            avatarUrl = avatarUrl,
            handle = handle,
            cookie = cookieHeader,
            sapisid = sapisid
        )

        try {
            val json = JSONObject().apply {
                put("is_logged_in", true)
                put("user_id", userId)
                put("name", userName)
                put("email", userEmail)
                put("avatar_url", avatarUrl ?: "")
                put("handle", handle ?: "")
                put("cookie", cookieHeader)
                put("sapisid", sapisid ?: "")
                put("saved_at", System.currentTimeMillis())
            }
            sessionFile.writeText(json.toString(2), Charsets.UTF_8)
            println("[DesktopAccountManager] Saved session to ${sessionFile.absolutePath}")
        } catch (e: Exception) {
            println("[DesktopAccountManager] Failed to write session file: ${e.message}")
        }

        _profile.value = newProfile
    }

    fun importToken(tokenString: String, defaultName: String = "Namikaze aftab"): Boolean {
        val clean = tokenString.trim()
        if (clean.isBlank()) return false

        // Check if raw cookie or token format
        var cookieHeader = clean
        var name = defaultName
        var avatar: String? = null

        if (clean.startsWith("{") && clean.endsWith("}")) {
            try {
                val json = JSONObject(clean)
                cookieHeader = json.optString("cookie", clean)
                name = json.optString("name", defaultName)
                avatar = json.optString("avatar").takeIf { it.isNotBlank() }
            } catch (_: Exception) {}
        }

        if (cookieHeader.contains("SAPISID") || cookieHeader.contains("__Secure-3PAPISID") || cookieHeader.contains("SSID") || cookieHeader.contains("LOGIN_INFO")) {
            saveSession(
                cookieHeader = cookieHeader,
                userName = name,
                userEmail = "Google Account Connected",
                avatarUrl = avatar
            )
            return true
        }

        // Even if just sapisid is provided
        if (clean.length > 20 && !clean.contains("=")) {
            saveSession(
                cookieHeader = "SAPISID=$clean; __Secure-3PAPISID=$clean",
                userName = defaultName,
                userEmail = "Session Token Connected"
            )
            return true
        }

        saveSession(cookieHeader = clean, userName = defaultName)
        return true
    }

    fun clearSession() {
        try {
            if (sessionFile.exists()) sessionFile.delete()
        } catch (_: Exception) {}
        _profile.value = DesktopUserProfile()
    }

    private fun extractCookiePart(cookie: String, key: String): String? {
        val regex = Regex("(?:^|;\\s*)$key=([^;]+)")
        return regex.find(cookie)?.groupValues?.get(1)
    }
}
