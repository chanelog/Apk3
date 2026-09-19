package hev.htproxy

/**
 * WAJIB: nama package (hev.htproxy) dan nama class (TProxyService) di file ini
 * harus SAMA PERSIS dengan yang tertulis di README resmi heiher/hev-socks5-tunnel,
 * karena native library di dalam hev-socks5-tunnel.aar (prebuilt) mendaftarkan
 * fungsi JNI-nya langsung ke hev.htproxy.TProxyService — bukan lewat nama app kita.
 * Jangan pindahkan/ganti nama file/package ini.
 *
 * Sumber: https://github.com/heiher/hev-socks5-tunnel#prebuilt-android-aar
 */
object TProxyService {
  external fun TProxyStartService(config_path: String, fd: Int): Boolean
  external fun TProxyStopService(): Boolean
  external fun TProxyIsRunning(): Boolean

  /** Hasil: [tx_packets, tx_bytes, rx_packets, rx_bytes] */
  external fun TProxyGetStats(): LongArray

  init {
    System.loadLibrary("hev-socks5-tunnel")
  }
}
