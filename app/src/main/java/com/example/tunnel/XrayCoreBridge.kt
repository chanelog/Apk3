package com.example.tunnel

import com.example.util.LogManager
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Jembatan ke Xray-core (libv2ray.aar dari 2dust/AndroidLibXrayLite).
 *
 * Revisi 4 — API-nya sudah dicek LANGSUNG dari isi file .class di dalam
 * libv2ray.aar (bukan dokumentasi/tebakan lagi), jadi nama & tipe di bawah
 * ini dijamin 100% cocok:
 *
 *   libv2ray.Libv2ray.newCoreController(CoreCallbackHandler): CoreController
 *   libv2ray.Libv2ray.initCoreEnv(String, String): void
 *   libv2ray.CoreCallbackHandler { onEmitStatus(Long,String):Long; shutdown():Long; startup():Long }
 *   libv2ray.CoreController { startLoop(String,Int); stopLoop(); isRunning: Boolean }
 *
 * SATU-SATUNYA hal yang masih belum 100% pasti: arti parameter kedua
 * (Int) di startLoop(configJson, ???) — saya pakai 0 sebagai default aman.
 * Kalau start() melempar error aneh yang menyebut domain strategy / IPv6 /
 * semacamnya, kemungkinan itu penyebabnya — kirim pesan errornya ke Claude.
 *
 * Tidak ada method protect()/setup() di interface ini — Xray-core jalan
 * satu proses dengan app kita, jadi builder.addDisallowedApplication(packageName)
 * di HnTunnelVpnService.kt sudah cukup meng-exclude trafik Xray dari TUN kita sendiri.
 */
object XrayCoreBridge {

  private var controller: CoreController? = null
  private var envInitialized = false

  private class Callback : CoreCallbackHandler {
    override fun onEmitStatus(status: Long, msg: String?): Long {
      LogManager.d("Xray status: $status ${msg ?: ""}")
      return 0
    }
    override fun shutdown(): Long = 0
    override fun startup(): Long = 0
  }

  @Throws(Exception::class)
  fun start(configJson: String, assetsPath: String) {
    if (!envInitialized) {
      Libv2ray.initCoreEnv(assetsPath, "")
      envInitialized = true
    }
    val ctrl = Libv2ray.newCoreController(Callback())
    ctrl.startLoop(configJson, 0)
    controller = ctrl
    LogManager.d("Xray startLoop dipanggil, menunggu status konek...")
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
    controller?.isRunning ?: false
  } catch (e: Exception) {
    false
  }
}
