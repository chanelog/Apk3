package com.example.tunnel

import android.content.Context
import java.io.File

/**
 * Runs the native Xray binary in SOCKS-only mode, matching the NetMod pattern:
 * VPN/TUN -> tun2socks -> local SOCKS 127.0.0.1:<port> -> Xray.
 */
object XrayProcessBridge {

  private var process: Process? = null
  private var configFile: File? = null

  fun start(context: Context, configJson: String, socksPort: Int): Boolean {
    stop()

    val binary = resolveBinary(context) ?: return false
    val dir = binary.parentFile ?: File(context.filesDir, "xray")
    if (!dir.exists()) dir.mkdirs()

    configFile = File(dir, "xray-config.json")
    configFile?.writeText(configJson)

    val builder = ProcessBuilder(
      binary.absolutePath,
      "-config",
      configFile!!.absolutePath
    )
      .redirectErrorStream(true)

    return try {
      process = builder.start()
      process?.isAlive == true
    } catch (_: Throwable) {
      false
    }
  }

  fun stop() {
    process?.destroy()
    process = null
    try {
      configFile?.delete()
    } catch (_: Throwable) {
      // no-op
    }
    configFile = null
  }

  fun isRunning(): Boolean = process?.isAlive == true

  private fun resolveBinary(context: Context): File? {
    val candidates = listOf(
      File(context.filesDir, "xray/xray"),
      File(context.cacheDir, "xray/xray"),
      File(context.getExternalFilesDir(null), "xray/xray"),
      File("/data/data/${context.packageName}/files/xray/xray")
    )

    for (candidate in candidates) {
      if (candidate.exists() && candidate.canExecute()) return candidate
      if (candidate.exists() && !candidate.canExecute()) {
        candidate.setExecutable(true, false)
        if (candidate.canExecute()) return candidate
      }
    }

    return null
  }
}
