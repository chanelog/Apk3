package com.example.tunnel

/**
 * ⚠️ BAGIAN PALING BERISIKO PERLU PENYESUAIAN — baca ini dulu sebelum build.
 *
 * Class ini memanggil `libv2ray.aar` (hasil gomobile bind dari
 * 2dust/AndroidLibXrayLite). Beda dengan hev-socks5-tunnel yang API Java-nya
 * stabil & terdokumentasi resmi, interface Kotlin di sini di-generate OTOMATIS
 * oleh gomobile dari kode Go — dan menurut histori issue di komunitasnya,
 * jumlah/tipe parameter constructor & method callback-nya BISA BEDA antar
 * versi rilis (lihat: github.com/2dust/v2rayNG/issues/88, orang lain juga
 * kena error "too many arguments").
 *
 * KEMUNGKINAN BESAR kode di bawah ini akan gagal compile dengan pesan mirip:
 *   "No value passed for parameter 'p1'"
 *   "too many/unresolved arguments for public open fun newV2RayPoint"
 *
 * CARA PERBAIKI kalau itu terjadi (bukan tanda ada yang salah fatal, ini
 * memang karakteristik binding gomobile):
 * 1. Baca pesan error compile Gradle-nya persis — dia akan bilang method mana
 *    dan parameter keberapa yang tidak cocok.
 * 2. Buka libv2ray.aar yang sudah didownload (app/libs/libv2ray.aar) pakai
 *    Android Studio > klik kanan > "Open Library" atau extract classes.jar
 *    lalu decompile, untuk lihat signature asli `Libv2ray.newV2RayPoint(...)`
 *    dan interface `V2RayVPNServiceSupportsSet` di versi yang kamu pakai.
 * 3. Sesuaikan jumlah parameter/nama method di class ini. Kerangka besarnya
 *    (constructor butuh objek callback, callback punya method shutdown/
 *    protect/setup/onEmitStatusChanged, V2RayPoint punya configureFileContent/
 *    runLoop/stopLoop) SANGAT KEMUNGKINAN besar tetap sama — yang beda
 *    biasanya cuma jumlah parameter di 1-2 method.
 * 4. Kirim pesan error compile-nya ke saya kalau butuh bantuan menyesuaikan.
 */
object XrayCoreBridge {

  private var v2rayPoint: Any? = null // libv2ray.V2RayPoint, di-cast reflektif di bawah supaya file ini tetap kompilasi
  // walau nama class persisnya berbeda versi. Kalau kamu sudah pastikan nama classnya,
  // ganti "Any" jadi tipe konkret libv2ray.V2RayPoint untuk type-safety penuh.

  /**
   * Callback yang WAJIB diimplementasikan Xray-core untuk bicara balik ke app
   * (minta izin protect() socket dari VpnService, dsb). Pola & nama method di
   * bawah ini mengikuti v2rayNG resmi — lihat catatan kelas di atas kalau ada
   * yang tidak cocok.
   */
  class Callback(
    private val protectSocket: (fd: Int) -> Boolean,
    private val onStatusChanged: (status: Long, message: String?) -> Unit
  ) {
    fun shutdown(): Long = 0
    fun prepare(): Long = 0
    fun protect(fd: Long): Boolean = protectSocket(fd.toInt())
    fun onEmitStatusChanged(status: Long, message: String?) = onStatusChanged(status, message)
    // Beberapa versi juga minta method seperti getStatsGetter()/getStatsGetterName()
    // untuk fitur query statistik — belum dipasang di sini karena bukan wajib
    // untuk sekadar bisa connect. Tambahkan kalau compiler memintanya.
  }

  /**
   * Mulai Xray-core dengan [configJson], dan [protectSocket] adalah fungsi
   * untuk memanggil VpnService.protect(fd) (WAJIB, supaya trafik Xray sendiri
   * tidak ikut ter-capture balik ke TUN interface / infinite loop).
   */
  @Throws(Exception::class)
  fun start(configJson: String, protectSocket: (fd: Int) -> Boolean) {
    // import di dalam fungsi supaya file ini tetap kompilasi sebelum libv2ray.aar
    // ditambahkan (memudahkan review) — hapus baris ini & ganti dengan import
    // biasa di atas file setelah dependency-nya aktif:
    //   import libv2ray.Libv2ray
    val libv2rayClass = Class.forName("libv2ray.Libv2ray")

    val callback = Callback(
      protectSocket = protectSocket,
      onStatusChanged = { status, message ->
        com.example.util.LogManager.d("Xray status: $status ${message ?: ""}")
      }
    )

    // Refleksi dipakai supaya file ini tidak gagal kompilasi total kalau nama
    // method sedikit beda — begitu kamu pastikan signature aslinya, GANTI
    // seluruh blok reflection ini dengan panggilan langsung, contoh:
    //   v2rayPoint = Libv2ray.newV2RayPoint(callback, true)
    //   (v2rayPoint as libv2ray.V2RayPoint).configureFileContent = configJson
    //   (v2rayPoint as libv2ray.V2RayPoint).runLoop(false)
    val newPointMethod = libv2rayClass.methods.firstOrNull { it.name == "newV2RayPoint" }
      ?: throw IllegalStateException(
        "Method newV2RayPoint tidak ditemukan di libv2ray.aar — cek nama class/method yang benar (lihat komentar di XrayCoreBridge.kt)"
      )
    val point = when (newPointMethod.parameterCount) {
      1 -> newPointMethod.invoke(null, callback)
      2 -> newPointMethod.invoke(null, callback, true)
      else -> throw IllegalStateException(
        "newV2RayPoint punya ${newPointMethod.parameterCount} parameter, tidak dikenali. Sesuaikan manual di XrayCoreBridge.kt"
      )
    }
    v2rayPoint = point

    val configSetter = point.javaClass.methods.firstOrNull {
      it.name.equals("setConfigureFileContent", ignoreCase = true) ||
        it.name.equals("setConfigureFile", ignoreCase = true)
    } ?: throw IllegalStateException("Setter config tidak ditemukan di V2RayPoint — cek nama field configureFile* di aar")
    configSetter.invoke(point, configJson)

    val runLoopMethod = point.javaClass.methods.firstOrNull { it.name == "runLoop" }
      ?: throw IllegalStateException("Method runLoop tidak ditemukan di V2RayPoint")
    when (runLoopMethod.parameterCount) {
      0 -> runLoopMethod.invoke(point)
      1 -> runLoopMethod.invoke(point, false)
      else -> throw IllegalStateException("runLoop punya parameter tidak dikenali, sesuaikan manual")
    }
  }

  fun stop() {
    val point = v2rayPoint ?: return
    try {
      point.javaClass.methods.firstOrNull { it.name == "stopLoop" }?.invoke(point)
    } catch (e: Exception) {
      com.example.util.LogManager.d("Error saat stop Xray: ${e.message}")
    }
    v2rayPoint = null
  }

  fun isRunning(): Boolean {
    val point = v2rayPoint ?: return false
    return try {
      point.javaClass.methods.firstOrNull { it.name == "isRunning" }?.invoke(point) as? Boolean ?: false
    } catch (e: Exception) {
      false
    }
  }
}
