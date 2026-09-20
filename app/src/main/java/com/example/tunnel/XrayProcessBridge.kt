package com.example.tunnel

import android.content.Context
import com.example.util.LogManager
import java.io.File

/** Runs the Xray ELF shipped in the APK native library directory. */
object XrayProcessBridge {
  private var process: Process? = null
  private var configFile: File? = null

  fun start(context: Context, configJson: String, socksPort: Int): Boolean {
    stop()

    // Do not execute from filesDir/cacheDir/assets: Android commonly mounts
    // those locations with noexec and returns EACCES even after chmod +x.
    val binary = File(context.applicationInfo.nativeLibraryDir, "libxray.so")
    if (!binary.isFile || !binary.canExecute()) {
      LogManager.e("Xray executable tidak ditemukan di ${binary.absolutePath}. Bundle Xray sebagai jniLibs/arm64-v8a/libxray.so.")
      return false
    }
    LogManager.d("Xray binary: ${binary.absolutePath}")

    val configDir = File(context.filesDir, "xray-config")
    if (!configDir.exists() && !configDir.mkdirs()) {
      LogManager.e("Tidak bisa membuat direktori konfigurasi Xray")
      return false
    }
    configFile = File(configDir, "config.json").apply { writeText(configJson) }

    return try {
      process = ProcessBuilder(
        binary.absolutePath,
        "run",
        "-config",
        configFile!!.absolutePath
      ).redirectErrorStream(true).start()

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

      Thread.sleep(200)
      if (process?.isAlive != true) {
        LogManager.e("Xray berhenti setelah start; lihat log Xray sebelumnya")
        stop()
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
}
