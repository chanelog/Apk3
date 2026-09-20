package com.example.tunnel

import android.content.Context
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs the native Xray binary in SOCKS-only mode, matching the NetMod pattern:
 * VPN/TUN -> tun2socks -> local SOCKS 127.0.0.1:<port> -> Xray.
 *
 * This is intentionally kept separate from the libv2ray AAR bridge because the
 * AAR-based direct TUN path has proven unreliable for this project when used with
 * a SOCKS inbound and custom routing setup.
 */
object XrayProcessBridge {

  private var process: Process? = null
  private var configFile: File? = null

  fun start(context: Context, configJson: String, socksPort: Int): Boolean {
    stop()

    val dir = File(context.filesDir, "xray")
    if (!dir.exists()) dir.mkdirs()

    val binary = File(dir, "xray")
    if (!binary.exists()) {
      // The workflow downloads the Android binary into app/src/main/assets/xray/xray
      // for debug builds. If it is absent, we cannot start the native core.
      return false
    }
    if (!binary.canExecute()) {
      binary.setExecutable(true, false)
    }

    configFile = File(dir, "xray-config.json")
    configFile?.writeText(configJson)

    val builder = ProcessBuilder(
      binary.absolutePath,
      "-config",
      configFile!!.absolutePath
    )
      .redirectErrorStream(true)

    try {
      process = builder.start()
      return process?.isAlive == true
    } catch (_: Throwable) {
      return false
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
}
