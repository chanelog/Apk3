# Ringkasan Patch — Fase 1 (tun2socks) + Fase 2 (Xray-core) + Fase 3 (SSH)
### Revisi 3 — Xray-core ditambahkan

## Cara pasang
Timpa/tambahkan file-file berikut di repo `chanelog/Apk3` kamu, lalu commit & push:

- `.github/workflows/build.yml` — diubah
- `app/build.gradle.kts` — diubah
- `app/src/main/java/com/example/service/HnTunnelVpnService.kt` — diubah
- `app/src/main/java/com/example/tunnel/SshSocksBridge.kt` — baru (SSH)
- `app/src/main/java/com/example/tunnel/HevSocks5Bridge.kt` — baru (tun2socks)
- `app/src/main/java/hev/htproxy/TProxyService.kt` — baru (WAJIB package/nama persis)
- `app/src/main/java/com/example/tunnel/XrayConfigBuilder.kt` — baru (Xray)
- `app/src/main/java/com/example/tunnel/XrayCoreBridge.kt` — baru (Xray, PALING BERISIKO)
- `README_INTEGRASI.md` — panduan ini

## Status tiap protokol
| Protokol | Status | Catatan |
|---|---|---|
| SSH_DIRECT | ✅ Sungguhan | JSch, paling stabil |
| SSH_SSL_TLS | ✅ Sungguhan | JSch + custom TLS/SNI proxy |
| SSH_HTTP_PROXY | ✅ Sungguhan | JSch + payload injection proxy |
| V2Ray/VMess | ⚠️ Sungguhan, BELUM TERVALIDASI COMPILE | lihat bagian Xray di bawah |
| V2Ray/VLESS | ⚠️ Sungguhan, BELUM TERVALIDASI COMPILE | sama seperti VMess |
| Trojan | ⚠️ Sungguhan, BELUM TERVALIDASI COMPILE | sama seperti VMess |

## Bagian Xray-core — BACA INI DULU
Beda dengan tun2socks (API resmi & stabil), interface Kotlin untuk memanggil
Xray-core (`libv2ray.aar` dari `2dust/AndroidLibXrayLite`) itu **di-generate
otomatis dari kode Go** (gomobile bind), dan menurut histori issue di
komunitasnya, jumlah/tipe parameter constructor-nya **bisa beda antar versi
rilis** — bahkan pengguna lain (bukan cuma saya yang nebak) pernah kena error
serupa dan harus menyesuaikan manual.

Untuk mengantisipasi ini, `XrayCoreBridge.kt` saya tulis **pakai reflection**
(bukan pemanggilan langsung) supaya:
1. Tetap bisa **compile** meski nama method sedikit beda.
2. Kalau runtime gagal karena method/parameter tidak ditemukan, akan muncul
   pesan error yang jelas (bukan crash misterius), dan saya sudah tulis
   instruksi persis apa yang perlu dicek di komentar file itu.

**Kalau APK gagal connect di jalur V2Ray/VLESS/Trojan**, kirim ke saya:
- Log dari `LogManager` (khususnya baris yang mengandung kata "Xray")
- Atau error compile Gradle-nya kalau gagal di tahap build

...dan saya bantu sesuaikan `XrayCoreBridge.kt`-nya berdasarkan pesan error
yang sebenarnya (jauh lebih akurat daripada saya menebak-nebak tanpa data).

## Yang perlu divalidasi (semua fase)
1. **URL download `.aar`** (baik hev-socks5-tunnel maupun libv2ray) pakai
   pattern `.../releases/latest/download/<nama-file>` — kalau 404, cek manual
   halaman Releases masing-masing repo untuk nama asset yang benar.
2. **`hev.htproxy.TProxyService`** — package/nama class harus persis, jangan diubah.
3. **`XrayCoreBridge.kt`** — lihat penjelasan di atas.
4. Statistik SSH (`bytesIn`/`bytesOut`) pakai angka asli dari `TProxyGetStats()`
   (dipakai bersama, baik untuk SSH maupun Xray, karena keduanya lewat
   tun2socks yang sama).

## Rekomendasi urutan testing
1. Build dulu, pastikan compile sukses (di sinilah `XrayCoreBridge.kt`
   kemungkinan besar perlu 1 kali penyesuaian).
2. Test profil **SSH** dulu (paling stabil) — pastikan tun2socks+SSH jalan.
3. Baru test profil **V2Ray/VLESS/Trojan**.
