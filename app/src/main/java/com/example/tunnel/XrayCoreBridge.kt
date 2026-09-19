package com.example.tunnel

/**
 * Xray-core DINONAKTIFKAN SEMENTARA (revisi 3).
 *
 * Dua percobaan sebelumnya (reflection dgn nama salah, lalu binding langsung
 * ke package "libv2ray") sama-sama gagal — compiler bilang package "libv2ray"
 * itu sendiri tidak ada di dalam libv2ray.aar. Berarti struktur paket
 * sebenarnya beda dari dugaan.
 *
 * Daripada nebak lagi (sudah 2x salah), workflow CI sekarang punya step baru
 * "Inspect libv2ray.aar contents" yang mem-print daftar class & method asli
 * di dalam file itu ke log Actions. Kirim isi log step itu ke Claude, baru
 * XrayCoreBridge ini ditulis ulang dengan nama yang PASTI benar.
 *
 * Untuk sementara jalur V2Ray/VLESS/Trojan sengaja gagal jelas di sini,
 * supaya SSH (yang sudah diperbaiki) bisa dites duluan tanpa APK gagal build.
 */
object XrayCoreBridge {
  @Throws(Exception::class)
  fun start(configJson: String, protectSocket: (fd: Int) -> Boolean) {
    throw UnsupportedOperationException(
      "Xray-core sementara dinonaktifkan — menunggu hasil inspeksi libv2ray.aar (lihat log CI step \"Inspect libv2ray.aar contents\")."
    )
  }

  fun stop() {}

  fun isRunning(): Boolean = false
}
