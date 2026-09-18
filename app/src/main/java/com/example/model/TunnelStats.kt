package com.example.model

data class TunnelStats(
  val bytesIn: Long = 0L,
  val bytesOut: Long = 0L,
  val speedInBps: Long = 0L,
  val speedOutBps: Long = 0L,
  val pingMs: Int = -1,
  val durationSeconds: Long = 0L
) {
  fun formatDuration(): String {
    val hrs = durationSeconds / 3600
    val mins = (durationSeconds % 3600) / 60
    val secs = durationSeconds % 60
    return if (hrs > 0) {
      String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
      String.format("%02d:%02d", mins, secs)
    }
  }

  fun formatBytes(bytes: Long): String {
    return when {
      bytes >= 1_073_741_824L -> String.format("%.2f GB", bytes / 1_073_741_824.0)
      bytes >= 1_048_576L -> String.format("%.2f MB", bytes / 1_048_576.0)
      bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
      else -> "$bytes B"
    }
  }

  fun formatSpeed(bps: Long): String {
    return when {
      bps >= 1_048_576L -> String.format("%.2f MB/s", bps / 1_048_576.0)
      bps >= 1024L -> String.format("%.1f KB/s", bps / 1024.0)
      else -> "$bps B/s"
    }
  }
}
