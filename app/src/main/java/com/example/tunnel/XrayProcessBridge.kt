package com.example.tunnel

import android.content.Context
import com.example.util.LogManager
import java.io.File

/** Runs the bundled Xray binary as a local SOCKS server. */
object XrayProcessBridge {
  private var process: Process? = null
  private var configFile: File? = null

  fun start(context: Context, configJson: String, socksPort: Int): Boolean {
    stop()

    val binary = installBundledBinary(context) ?: run {
      LogManager.e("Xray binary tidak ditemukan di assets/xray/xray")
      return false
    }

    val dir = binary.parentFile ?: context.filesDir
    configFile = File(dir, "xray-config.json").apply { writeText(configJson) }

    return try {
      process = ProcessBuilder(
        binary.absolutePath,
        "run",
        "-config",
        configFile!!.absolutePath
      ).redirectErrorStream(true).start()

      // Always consume native output; otherwise the process can block when its
      // stdout/stderr pipe becomes full. Keep useful diagnostics in app logs.
      Thread {
        try {
          process?.inputStream?.bufferedReader()?.forEachLine { line ->
            LogManager.d("Xray: $line")
          }
        } catch (_: Exception) {
          // Process is stopping.
        }
      }.apply {
        name = "xray-log-reader"
        isDaemon = true
        start()
      }

      Thread.sleep(150)
      if (process?.isAlive != true) {
        LogManager.e("Xray berhenti setelah start; cek log Xray di atas")
        false
      } else {
        LogManager.s("Xray native process aktif, SOCKS port=$socksPort")
        true
      }
    } catch (e: Exception) {
      LogManager.e("Gagal menjalankan Xray: ${e.message}")
      stop()
      false
    }
  }

  fun stop() {
    try { process?.destroy() } catch (_: Exception) {}
    process = null
    try { configFile?.delete() } catch (_: Exception) {}
    configFile = null
  }

  fun isRunning(): Boolean = process?.isAlive == true

  private fun installBundledBinary(context: Context): File? {
    val target = File(context.filesDir, "xray/xray")
    if (target.exists()) {
      target.setExecutable(true, false)
      if (target.canExecute()) return target
    }

    return try {
      val parent = target.parentFile ?: return null
      if (!parent.exists()) parent.mkdirs()
      context.assets.open("xray/xray").use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
      }
      target.setReadable(true, false)
      target.setExecutable(true, false)
      if (target.canExecute()) target else null
    } catch (e: Exception) {
      LogManager.e("Gagal menyalin binary Xray dari assets: ${e.message}")
      null
    }
  }
}
