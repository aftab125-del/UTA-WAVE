package com.uta.wave.data.account

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val name: String = "Guest User",
    val email: String = "Offline Mode",
    val avatarUrl: String? = null,
    val cookie: String? = null,
    val sapisid: String? = null
)

class AccountManager private constructor(context: Context) {

    companion object {
        private const val PREFS_ACCOUNT = "uta_wave_account"

        @Volatile
        private var INSTANCE: AccountManager? = null

        fun getInstance(context: Context): AccountManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccountManager(context.applicationContext).also { INSTANCE = it }
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
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_ACCOUNT, Context.MODE_PRIVATE)

    private val _profileFlow = MutableStateFlow(loadProfile())
    val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

    val currentProfile: UserProfile
        get() = _profileFlow.value

    private fun loadProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) {
            return UserProfile()
        }
        val name = prefs.getString("user_name", "Music Explorer") ?: "Music Explorer"
        val email = prefs.getString("user_email", "Google Account") ?: "Google Account"
        val avatarUrl = prefs.getString("avatar_url", null)
        val cookie = prefs.getString("cookie_header", null)
        val sapisid = prefs.getString("sapisid", null)

        return UserProfile(
            isLoggedIn = true,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            cookie = cookie,
            sapisid = sapisid
        )
    }

    fun saveSession(
        cookieHeader: String,
        userName: String = "Music Explorer",
        userEmail: String = "Google Account",
        avatarUrl: String? = null
    ) {
        val sapisid = extractCookieValue(cookieHeader, "SAPISID")
            ?: extractCookieValue(cookieHeader, "__Secure-3PAPISID")

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_name", userName)
            .putString("user_email", userEmail)
            .putString("avatar_url", avatarUrl)
            .putString("cookie_header", cookieHeader)
            .putString("sapisid", sapisid)
            .apply()

        _profileFlow.value = UserProfile(
            isLoggedIn = true,
            name = userName,
            email = userEmail,
            avatarUrl = avatarUrl,
            cookie = cookieHeader,
            sapisid = sapisid
        )
    }

    fun logout() {
        prefs.edit().clear().apply()
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            // ignore
        }
        _profileFlow.value = UserProfile()
    }

    private fun extractCookieValue(cookieHeader: String, key: String): String? {
        val parts = cookieHeader.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("$key=")) {
                return trimmed.substring(key.length + 1)
            }
        }
        return null
    }
}
