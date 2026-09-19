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
 * Revisi 5 — ketemu dokumentasi resmi terbaru (pkg.go.dev, published Sep 2026)
 * yang mengungkap parameter kedua StartLoop itu adalah TUN FILE DESCRIPTOR,
 * bukan flag sembarang:
 *
 *   func (x *CoreController) StartLoop(configContent string, tunFd int32) (err error)
 *
 * Artinya versi Xray-core ini sudah punya tun2socks BAWAAN sendiri di
 * dalamnya — beda dari dugaan awal (Xray cuma buka SOCKS lokal, lalu
 * dijembatani tun2socks terpisah). Untuk jalur Xray, HevSocks5Bridge
 * (tun2socks) TIDAK dipakai sama sekali — Xray langsung pegang TUN fd-nya.
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
  fun start(configJson: String, tunFd: Int, assetsPath: String) {
    if (!envInitialized) {
      Libv2ray.initCoreEnv(assetsPath, "")
      envInitialized = true
    }
    val ctrl = Libv2ray.newCoreController(Callback())
    ctrl.startLoop(configJson, tunFd)
    controller = ctrl
    LogManager.d("Xray startLoop dipanggil dengan tunFd=$tunFd, menunggu status konek...")
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
