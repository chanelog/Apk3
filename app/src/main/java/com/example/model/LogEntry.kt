package com.example.model

import java.util.concurrent.atomic.AtomicLong

enum class LogLevel {
  INFO,
  DEBUG,
  SUCCESS,
  WARN,
  ERROR
}

private val globalLogIdSequence = AtomicLong(1)

data class LogEntry(
  val id: Long = globalLogIdSequence.getAndIncrement(),
  val timestamp: String,
  val level: LogLevel,
  val message: String
)
