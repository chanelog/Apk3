package com.example.tunnel

import com.example.util.LogManager
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet

/**
 * Jembatan ke Xray-core (libv2ray.aar dari 2dust/AndroidLibXrayLite).
 *
 * Revisi 2 — sebelumnya pakai reflection dan salah tebak nama method
 * (newV2RayPoint, huruf kecil). Sekarang dipanggil LANGSUNG berdasarkan
 * dokumentasi resmi Go-nya (pkg.go.dev/github.com/2dust/AndroidLibXrayLite):
 *
 *   func NewV2RayPoint(s V2RayVPNServiceSupportsSet, adns bool) *V2RayPoint
 *   type V2RayVPNServiceSupportsSet interface {
 *       Setup(Conf string) int
 *       Prepare() int
 *       Shutdown() int
 *       Protect(int) bool
 *       OnEmitStatus(int, string) int
 *   }
 *   func (v *V2RayPoint) RunLoop(prefIPv6 bool) (err error)
 *   func (v *V2RayPoint) StopLoop() (err error)
 *
 * Catatan tipe: gomobile bind memetakan tipe Go `int` ke Java `long`, jadi
 * parameter/return yang di Go bertipe int, di Kotlin jadi Long.
 *
 * KALAU MASIH GAGAL COMPILE: pesan error Kotlin compiler akan menunjukkan
 * PERSIS signature yang diharapkan (seperti kasus SocketFactory di JSch
 * kemarin) — cukup sesuaikan nama/tipe di file ini sesuai pesan errornya,
 * lalu kirim pesan errornya ke Claude kalau butuh bantuan.
 */
object XrayCoreBridge {

  private var v2rayPoint: V2RayPoint? = null

  private class Callback(
    private val protectSocket: (fd: Int) -> Boolean
  ) : V2RayVPNServiceSupportsSet {
    override fun Setup(Conf: String?): Long = 0
    override fun Prepare(): Long = 0
    override fun Shutdown(): Long = 0
    override fun Protect(fd: Long): Boolean = protectSocket(fd.toInt())
    override fun OnEmitStatus(status: Long, msg: String?): Long {
      LogManager.d("Xray status: $status ${msg ?: ""}")
      return 0
    }
  }

  /**
   * Mulai Xray-core dengan [configJson]. [protectSocket] WAJIB dihubungkan ke
   * VpnService.protect(fd) supaya trafik Xray sendiri tidak ikut ter-capture
   * balik ke TUN interface (infinite loop).
   */
  @Throws(Exception::class)
  fun start(configJson: String, protectSocket: (fd: Int) -> Boolean) {
    val point = Libv2ray.NewV2RayPoint(Callback(protectSocket), false)
    point.configureFileContent = configJson
    point.runLoop(false)
    v2rayPoint = point
    LogManager.d("Xray RunLoop dipanggil, menunggu status konek...")
  }

  fun stop() {
    try {
      v2rayPoint?.stopLoop()
    } catch (e: Exception) {
      LogManager.d("Error saat stop Xray: ${e.message}")
    }
    v2rayPoint = null
  }

  fun isRunning(): Boolean = try {
    v2rayPoint?.isRunning ?: false
  } catch (e: Exception) {
    false
  }
}
