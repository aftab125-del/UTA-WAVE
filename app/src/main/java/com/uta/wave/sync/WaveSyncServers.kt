package com.uta.wave.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SyncServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object WaveSyncServers {
    private const val SERVER_JSON_URL =
        "https://raw.githubusercontent.com/EchoMusicApp/Echo-Music/refs/heads/main/app/server.json"

    val DEFAULT_SERVERS = listOf(
        SyncServer(
            name = "Metrolist Relay (Primary)",
            url = "wss://metroserverx.meowery.eu/ws",
            location = "Poland / Europe",
            operator = "Metrolist"
        ),
        SyncServer(
            name = "Hugging Face Sync (Fallback)",
            url = "wss://devilmi-vivi-music-listen-together.hf.space",
            location = "Global",
            operator = "Echo Community"
        )
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _servers = MutableStateFlow(DEFAULT_SERVERS)
    val serversFlow: StateFlow<List<SyncServer>> = _servers.asStateFlow()

    val servers: List<SyncServer>
        get() = _servers.value

    val defaultServerUrl: String
        get() = DEFAULT_SERVERS.first().url

    init {
        refreshServers()
    }

    fun refreshServers() {
        scope.launch {
            try {
                val request = Request.Builder().url(SERVER_JSON_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use

                    val json = JSONObject(body)
                    val name = json.optString("name", "Dynamic Relay")
                    val serverUrl = json.optString("serverUrl", "")
                    val region = json.optString("region", "Global")

                    if (serverUrl.isNotEmpty()) {
                        val dynamicServer = SyncServer(
                            name = name,
                            url = serverUrl,
                            location = region,
                            operator = "Community"
                        )
                        // Prepend dynamic server if not already present
                        val current = _servers.value.filter { it.url != serverUrl }
                        _servers.value = listOf(dynamicServer) + current
                    }
                }
            } catch (_: Exception) {
                // Keep default servers fallback
            }
        }
    }

    fun findByUrl(url: String): SyncServer? = servers.firstOrNull { it.url == url }
}
