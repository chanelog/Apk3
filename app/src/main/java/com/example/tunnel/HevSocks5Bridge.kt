package com.example.tunnel

import android.content.Context
import hev.htproxy.TProxyService
import java.io.File

/**
 * Wrapper di atas library resmi hev-socks5-tunnel (lihat hev.htproxy.TProxyService).
 * Nama fungsi native di sini sudah dicocokkan langsung dari README resmi
 * https://github.com/heiher/hev-socks5-tunnel (bagian "Prebuilt Android AAR"),
 * jadi ini BUKAN tebakan lagi seperti versi sebelumnya.
 */
object HevSocks5Bridge {

  /** @return true kalau tunnel berhasil dimulai. */
  fun start(context: Context, socksPort: Int, mtu: Int = 1500, enableUdp: Boolean = true, tunFd: Int): Boolean {
    val configFile = writeConfig(context, socksPort, mtu, enableUdp)
    return TProxyService.TProxyStartService(configFile.absolutePath, tunFd)
  }

  fun stop(): Boolean = TProxyService.TProxyStopService()

  fun isRunning(): Boolean = TProxyService.TProxyIsRunning()

  /** @return [txPackets, txBytes, rxPackets, rxBytes] atau null kalau tunnel belum jalan. */
  fun stats(): LongArray? = try {
    TProxyService.TProxyGetStats()
  } catch (e: Exception) {
    null
  }

  private fun writeConfig(context: Context, socksPort: Int, mtu: Int, enableUdp: Boolean): File {
    val yaml = """
      tunnel:
        name: tun0
        mtu: $mtu
        ipv4: 10.8.0.2
      socks5:
        port: $socksPort
        address: 127.0.0.1
        udp: '${if (enableUdp) "udp" else "tcp"}'
    """.trimIndent()
    val file = File(context.filesDir, "hev-socks5-tunnel.yaml")
    file.writeText(yaml)
    return file
  }
}
