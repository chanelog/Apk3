package com.example.util

import com.example.model.LogEntry
import com.example.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {

  private const val MAX_LOGS = 300
  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  @Synchronized
  fun log(level: LogLevel, message: String) {
    val timeString = try {
      SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    } catch (e: Exception) {
      "00:00:00"
    }

    val entry = LogEntry(
      timestamp = timeString,
      level = level,
      message = message
    )

    _logs.update { currentList ->
      val updated = ArrayList(currentList)
      updated.add(entry)
      if (updated.size > MAX_LOGS) {
        updated.removeAt(0)
      }
      updated
    }
  }

  fun i(msg: String) = log(LogLevel.INFO, msg)
  fun d(msg: String) = log(LogLevel.DEBUG, msg)
  fun s(msg: String) = log(LogLevel.SUCCESS, msg)
  fun w(msg: String) = log(LogLevel.WARN, msg)
  fun e(msg: String) = log(LogLevel.ERROR, msg)

  fun clear() {
    _logs.value = emptyList()
  }

  fun getAllLogsAsText(): String {
    return _logs.value.joinToString("\n") { "[${it.timestamp}] [${it.level}] ${it.message}" }
  }
}
