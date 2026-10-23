package com.uta.wave.player

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String, // "INFO", "WARN", "ERROR"
    val message: String
)

object AppLogger {
    private const val TAG = "UTA_DEBUG"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    fun i(msg: String) {
        val entry = LogEntry(timeFormat.format(Date()), "INFO", msg)
        Log.i(TAG, msg)
        appendLog(entry)
    }

    fun w(msg: String) {
        val entry = LogEntry(timeFormat.format(Date()), "WARN", msg)
        Log.w(TAG, msg)
        appendLog(entry)
    }

    fun e(msg: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$msg: ${throwable.localizedMessage}" else msg
        val entry = LogEntry(timeFormat.format(Date()), "ERROR", fullMsg)
        Log.e(TAG, fullMsg, throwable)
        _lastError.value = fullMsg
        appendLog(entry)
    }

    private fun appendLog(entry: LogEntry) {
        _logs.update { current ->
            (listOf(entry) + current).take(60) // Keep last 60 entries
        }
    }

    fun clearLastError() {
        _lastError.value = null
    }

    fun getExportText(): String {
        return _logs.value.reversed().joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
    }
}
