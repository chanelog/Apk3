package com.example.tunnel

import com.example.util.LogManager
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/** Bridge to the AndroidLibXrayLite Xray core. */
object XrayCoreBridge {

  private var controller: CoreController? = null
  private var envInitialized = false

  private class Callback : CoreCallbackHandler {
    override fun onEmitStatus(status: Long, msg: String?): Long {
      LogManager.d("Xray status: $status ${msg.orEmpty()}")
      return 0
    }

    override fun shutdown(): Long {
      LogManager.d("Xray callback: shutdown")
      return 0
    }

    override fun startup(): Long {
      LogManager.d("Xray callback: startup")
      return 0
    }
  }

  /** Starts Xray with the already-established Android VPN TUN file descriptor. */
  @Throws(Exception::class)
  fun start(configJson: String, tunFd: Int, assetsPath: String) {
    stop()

    if (tunFd < 0) throw IllegalArgumentException("Invalid TUN file descriptor: $tunFd")
    if (!envInitialized) {
      Libv2ray.initCoreEnv(assetsPath, "")
      envInitialized = true
    }

    val ctrl = Libv2ray.newCoreController(Callback())
    ctrl.startLoop(configJson, tunFd)
    controller = ctrl

    // startLoop is asynchronous. Do not report a connected tunnel until the
    // native controller actually reports itself as running.
    runBlocking {
      repeat(20) {
        if (ctrl.isRunning) return@runBlocking
        delay(50)
      }
    }
    if (!ctrl.isRunning) {
      controller = null
      try { ctrl.stopLoop() } catch (_: Exception) {}
      throw IllegalStateException("Xray startLoop returned, but Xray is not running")
    }
    LogManager.s("Xray startLoop aktif dengan tunFd=$tunFd")
  }

  fun stop() {
    try {
      controller?.stopLoop()
    } catch (e: Exception) {
      LogManager.d("Error saat stop Xray: ${e.message}")
    }
    controller = null
  }

  fun isRunning(): Boolean = try {
    controller?.isRunning == true
  } catch (_: Exception) {
    false
  }
}
