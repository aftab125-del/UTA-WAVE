package com.uta.wave.data.account

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import com.uta.wave.data.innertube.InnerTubeClient
import com.uta.wave.player.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val name: String = "Guest User",
    val email: String = "Offline Mode",
    val avatarUrl: String? = null,
    val handle: String? = null,
    val cookie: String? = null,
    val sapisid: String? = null,
    val moreContentEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true
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

    private val scope = CoroutineScope(Dispatchers.IO)
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
        val name = prefs.getString("user_name", "Namikaze aftab") ?: "Namikaze aftab"
        val email = prefs.getString("user_email", "Google Account Connected") ?: "Google Account Connected"
        val avatarUrl = prefs.getString("avatar_url", null)
        val handle = prefs.getString("user_handle", null)
        val cookie = prefs.getString("cookie_header", null)
        val sapisid = prefs.getString("sapisid", null)
        val moreContent = prefs.getBoolean("more_content_enabled", true)
        val autoSync = prefs.getBoolean("auto_sync_enabled", true)

        return UserProfile(
            isLoggedIn = true,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            handle = handle,
            cookie = cookie,
            sapisid = sapisid,
            moreContentEnabled = moreContent,
            autoSyncEnabled = autoSync
        )
    }

    fun saveSession(
        cookieHeader: String,
        userName: String = "Namikaze aftab",
        userEmail: String = "Google Account Connected",
        avatarUrl: String? = null,
        handle: String? = null
    ) {
        val sapisid = extractCookieValue(cookieHeader, "SAPISID")
            ?: extractCookieValue(cookieHeader, "__Secure-3PAPISID")

        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_name", userName)
            .putString("user_email", userEmail)
            .putString("avatar_url", avatarUrl)
            .putString("user_handle", handle)
            .putString("cookie_header", cookieHeader)
            .putString("sapisid", sapisid)
            .apply()

        _profileFlow.value = UserProfile(
            isLoggedIn = true,
            name = userName,
            email = userEmail,
            avatarUrl = avatarUrl,
            handle = handle,
            cookie = cookieHeader,
            sapisid = sapisid,
            moreContentEnabled = _profileFlow.value.moreContentEnabled,
            autoSyncEnabled = _profileFlow.value.autoSyncEnabled
        )

        InnerTubeClient.authCookie = cookieHeader
        InnerTubeClient.authSapisid = sapisid

        // Asynchronously refresh real channel avatar and name from InnerTube
        scope.launch {
            refreshProfileFromYouTube()
        }
    }

    suspend fun refreshProfileFromYouTube() = withContext(Dispatchers.IO) {
        val current = _profileFlow.value
        val cookie = current.cookie ?: return@withContext
        try {
            val innerTube = InnerTubeClient()
            val info = innerTube.fetchAccountProfile(cookie, current.sapisid)
            if (info != null) {
                AppLogger.i("Updated profile from YouTube: ${info.name}, avatar: ${info.avatarUrl}")
                val updatedName = if (info.name.isNotBlank()) info.name else current.name
                val updatedEmail = if (info.email.isNotBlank()) info.email else current.email
                val updatedAvatar = info.avatarUrl ?: current.avatarUrl
                val updatedHandle = info.handle ?: current.handle

                prefs.edit()
                    .putString("user_name", updatedName)
                    .putString("user_email", updatedEmail)
                    .putString("avatar_url", updatedAvatar)
                    .putString("user_handle", updatedHandle)
                    .apply()

                _profileFlow.value = current.copy(
                    name = updatedName,
                    email = updatedEmail,
                    avatarUrl = updatedAvatar,
                    handle = updatedHandle
                )
            }
        } catch (e: Exception) {
            AppLogger.e("refreshProfileFromYouTube failed", e)
        }
    }

    fun updateMoreContent(enabled: Boolean) {
        prefs.edit().putBoolean("more_content_enabled", enabled).apply()
        _profileFlow.value = _profileFlow.value.copy(moreContentEnabled = enabled)
    }

    fun updateAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
        _profileFlow.value = _profileFlow.value.copy(autoSyncEnabled = enabled)
    }

    fun updateProfile(name: String, email: String, avatarUrl: String?, handle: String? = null) {
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("avatar_url", avatarUrl)
            .putString("user_handle", handle)
            .apply()

        _profileFlow.value = _profileFlow.value.copy(
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            handle = handle
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
        InnerTubeClient.authCookie = null
        InnerTubeClient.authSapisid = null
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
