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

    // Android commonly mounts filesDir/cacheDir with noexec. A native binary
    // copied there therefore fails with EACCES even after chmod. Prefer the
    // APK-extracted nativeLibraryDir, which is executable by Android.
    val binary = resolveBinary(context) ?: run {
      LogManager.e("Xray binary tidak ditemukan. Letakkan libxray.so di jniLibs/arm64-v8a atau assets/xray/xray")
      return false
    }
    LogManager.d("Xray binary: ${binary.absolutePath}")

    val configDir = File(context.filesDir, "xray-config")
    if (!configDir.exists()) configDir.mkdirs()
    configFile = File(configDir, "config.json").apply { writeText(configJson) }

    return try {
      // Xray 25.x uses the explicit `run` subcommand.
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

  private fun resolveBinary(context: Context): File? {
    val nativeBinary = File(context.applicationInfo.nativeLibraryDir, "libxray.so")
    if (nativeBinary.exists() && nativeBinary.canExecute()) return nativeBinary

    // Fallback for development builds where the binary is already installed.
    val installed = File(context.filesDir, "xray/xray")
    if (installed.exists() && installed.canExecute()) return installed

    // Assets are copied only as a fallback. filesDir may be mounted noexec;
    // this path is retained to produce a useful diagnostic if so.
    return try {
      if (!installed.exists()) {
        installed.parentFile?.mkdirs()
        context.assets.open("xray/xray").use { input ->
          installed.outputStream().use { output -> input.copyTo(output) }
        }
        installed.setExecutable(true, false)
      }
      if (installed.canExecute()) installed else {
        LogManager.e("Binary Xray berada di filesDir tetapi filesystem noexec; gunakan libxray.so di nativeLibraryDir")
        null
      }
    } catch (e: Exception) {
      LogManager.e("Gagal menyiapkan binary Xray: ${e.message}")
      null
    }
  }
}
