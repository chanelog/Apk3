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
    val binary = File(context.applicationInfo.nativeLibraryDir, "libxray.so")
    if (!binary.isFile || !binary.canExecute()) {
      LogManager.e("Xray executable tidak ditemukan di ${binary.absolutePath}. Build harus memasukkan jniLibs/arm64-v8a/libxray.so")
      return false
    }
    val dir = File(context.filesDir, "xray-config")
    if (!dir.exists() && !dir.mkdirs()) return false
    configFile = File(dir, "config.json").apply { writeText(configJson) }
    return try {
      process = ProcessBuilder(binary.absolutePath, "run", "-config", configFile!!.absolutePath)
        .redirectErrorStream(true)
        .start()
      Thread {
        try { process?.inputStream?.bufferedReader()?.forEachLine { LogManager.d("Xray: $it") } }
        catch (_: Exception) { }
      }.apply { name = "xray-log-reader"; isDaemon = true; start() }
      Thread.sleep(250)
      if (process?.isAlive != true) {
        LogManager.e("Xray berhenti setelah start")
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
    try { process?.destroy() } catch (_: Exception) { }
    process = null
    try { configFile?.delete() } catch (_: Exception) { }
    configFile = null
  }

  fun isRunning(): Boolean = process?.isAlive == true
}
