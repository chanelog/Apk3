package com.example.tunnel

import com.example.model.TunnelConfig
import com.example.model.TunnelType
import com.example.util.LogManager
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Proxy
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import com.jcraft.jsch.SocketFactory
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Menjalankan koneksi SSH sungguhan (via JSch, dependency com.github.mwiede:jsch)
 * lalu membuka SOCKS5 server lokal (mirip "ssh -D") sehingga trafik dari
 * tun2socks (HevSocks5Bridge) bisa dialirkan lewat tunnel SSH.
 *
 * PENTING: ini baru meng-cover mode SSH_DIRECT, SSH_SSL_TLS, dan SSH_HTTP_PROXY.
 * V2Ray/VLESS/Trojan (via Xray-core) BELUM ditangani di sini — itu Fase 2 terpisah.
 */
class SshSocksBridge(private val localSocksPort: Int) {

  private var session: Session? = null
  private var serverSocket: ServerSocket? = null
  private val scope = CoroutineScope(Dispatchers.IO + Job())
  private var acceptJob: Job? = null

  @Volatile var isConnected: Boolean = false
    private set

  /** Membuka koneksi SSH sesuai TunnelType, lalu mulai SOCKS5 server lokal.
   * [protectSocket] WAJIB terhubung ke VpnService.protect(socket) supaya
   * socket kontrol SSH ini tidak ikut "tersedot" balik ke TUN interface
   * (baik punya sendiri maupun sisa sesi VPN lain yang masih aktif). */
  @Throws(Exception::class)
  fun start(config: TunnelConfig, protectSocket: (Socket) -> Boolean) {
    val jsch = JSch()
    val newSession = jsch.getSession(config.sshUsername, config.sshHost, config.sshPort)
    newSession.setConfig("StrictHostKeyChecking", "no")
    newSession.setPassword(config.sshPassword)
    newSession.timeout = 15000

    when (config.type) {
      TunnelType.SSH_SSL_TLS -> {
        LogManager.d("Menyiapkan proxy TLS+SNI ke ${config.sniHost} sebelum handshake SSH...")
        newSession.setProxy(
          TlsSniProxy(
            connectHost = config.sshHost,
            connectPort = config.sshPort,
            sniHost = config.sniHost,
            allowInsecure = true,
            protectSocket = protectSocket
          )
        )
      }
      TunnelType.SSH_HTTP_PROXY -> {
        val proxyTarget = parseHostPort(config.remoteProxy, config.sshHost, config.sshPort)
        LogManager.d("Menyiapkan payload proxy ke ${proxyTarget.first}:${proxyTarget.second}...")
        newSession.setProxy(
          PayloadInjectProxy(
            proxyHost = proxyTarget.first,
            proxyPort = proxyTarget.second,
            payloadTemplate = config.payload,
            targetHost = config.sshHost,
            targetPort = config.sshPort,
            protectSocket = protectSocket
          )
        )
      }
      TunnelType.SSH_DIRECT -> {
        newSession.setProxy(
          DirectProxy(
            connectHost = config.sshHost,
            connectPort = config.sshPort,
            protectSocket = protectSocket
          )
        )
      }
      else -> throw IllegalArgumentException("SshSocksBridge dipanggil untuk tipe non-SSH: ${config.type}")
    }

    newSession.connect()
    session = newSession
    isConnected = true
    LogManager.s("Sesi SSH berhasil terbentuk ke ${config.sshHost}:${config.sshPort}")

    startSocksServer()
  }

  private fun startSocksServer() {
    val server = ServerSocket(localSocksPort, 50, java.net.InetAddress.getByName("127.0.0.1"))
    serverSocket = server
    acceptJob = scope.launch {
      while (isActive) {
        val client = try {
          server.accept()
        } catch (e: IOException) {
          break
        }
        launch { handleSocksClient(client) }
      }
    }
  }

  private suspend fun handleSocksClient(client: Socket) {
    try {
      client.tcpNoDelay = true
      val input = client.getInputStream()
      val output = client.getOutputStream()

      val ver = input.read()
      if (ver != 0x05) { client.close(); return }
      val nMethods = input.read()
      val methods = ByteArray(nMethods)
      readFully(input, methods)
      output.write(byteArrayOf(0x05, 0x00))
      output.flush()

      val reqVer = input.read()
      val cmd = input.read()
      input.read()
      val atyp = input.read()
      if (reqVer != 0x05 || cmd != 0x01) {
        output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        client.close()
        return
      }

      val targetHost: String = when (atyp) {
        0x01 -> {
          val addr = ByteArray(4); readFully(input, addr)
          addr.joinToString(".") { (it.toInt() and 0xFF).toString() }
        }
        0x03 -> {
          val len = input.read()
          val domain = ByteArray(len); readFully(input, domain)
          String(domain, Charsets.US_ASCII)
        }
        0x04 -> {
          val addr = ByteArray(16); readFully(input, addr)
          java.net.InetAddress.getByAddress(addr).hostAddress ?: ""
        }
        else -> { client.close(); return }
      }
      val portBytes = ByteArray(2); readFully(input, portBytes)
      val targetPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

      val activeSession = session
      if (activeSession == null || !activeSession.isConnected) {
        output.write(byteArrayOf(0x05, 0x01, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        client.close()
        return
      }

      val channel = activeSession.openChannel("direct-tcpip") as ChannelDirectTCPIP
      channel.setHost(targetHost)
      channel.setPort(targetPort)
      channel.connect(10000)

      output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
      output.flush()

      pipe(client, channel)
    } catch (e: Exception) {
      LogManager.d("SOCKS session error: ${e.message}")
      try { client.close() } catch (ignored: Exception) {}
    }
  }

  private suspend fun pipe(client: Socket, channel: Channel) {
    val channelIn = channel.inputStream
    val channelOut = channel.outputStream
    val clientIn = client.getInputStream()
    val clientOut = client.getOutputStream()

    val toChannel = scope.launch {
      try { clientIn.copyTo(channelOut, bufferSize = 16384) } catch (ignored: Exception) {}
    }
    val toClient = scope.launch {
      try { channelIn.copyTo(clientOut, bufferSize = 16384) } catch (ignored: Exception) {}
    }
    toChannel.join()
    toClient.join()
    try { channel.disconnect() } catch (ignored: Exception) {}
    try { client.close() } catch (ignored: Exception) {}
  }

  private fun readFully(input: InputStream, buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
      val read = input.read(buffer, offset, buffer.size - offset)
      if (read < 0) throw IOException("Stream tertutup saat membaca SOCKS handshake")
      offset += read
    }
  }

  private fun parseHostPort(raw: String, fallbackHost: String, fallbackPort: Int): Pair<String, Int> {
    if (raw.isBlank()) return fallbackHost to fallbackPort
    val parts = raw.split(":")
    return if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: fallbackPort) else raw to fallbackPort
  }

  fun stop() {
    isConnected = false
    acceptJob?.cancel()
    try { serverSocket?.close() } catch (ignored: Exception) {}
    try { session?.disconnect() } catch (ignored: Exception) {}
    session = null
  }

  private class DirectProxy(
    private val connectHost: String,
    private val connectPort: Int,
    private val protectSocket: (Socket) -> Boolean
  ) : Proxy {
    private lateinit var socket: Socket

    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) {
      val raw = Socket()
      protectSocket(raw)
      raw.connect(InetSocketAddress(connectHost, connectPort), timeout)
      socket = raw
    }

    override fun getInputStream(): InputStream = socket.getInputStream()
    override fun getOutputStream(): OutputStream = socket.getOutputStream()
    override fun getSocket(): Socket = socket
    override fun close() {
      try { socket.close() } catch (ignored: Exception) {}
    }
  }

  private class TlsSniProxy(
    private val connectHost: String,
    private val connectPort: Int,
    private val sniHost: String,
    private val allowInsecure: Boolean,
    private val protectSocket: (Socket) -> Boolean
  ) : Proxy {
    private lateinit var socket: Socket

    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) {
      val raw = Socket()
      protectSocket(raw)
      raw.connect(InetSocketAddress(connectHost, connectPort), timeout)

      val sslContext = SSLContext.getInstance("TLS")
      if (allowInsecure) {
        val trustAll = object : X509TrustManager {
          override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
          override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
          override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        sslContext.init(null, arrayOf(trustAll), SecureRandom())
      } else {
        sslContext.init(null, null, SecureRandom())
      }

      val ssl = sslContext.socketFactory.createSocket(raw, sniHost, connectPort, true) as SSLSocket
      val params = ssl.sslParameters
      params.serverNames = listOf(SNIHostName(sniHost))
      ssl.sslParameters = params
      ssl.startHandshake()
      socket = ssl
    }

    override fun getInputStream(): InputStream = socket.getInputStream()
    override fun getOutputStream(): OutputStream = socket.getOutputStream()
    override fun getSocket(): Socket = socket
    override fun close() {
      try { socket.close() } catch (ignored: Exception) {}
    }
  }

  private class PayloadInjectProxy(
    private val proxyHost: String,
    private val proxyPort: Int,
    private val payloadTemplate: String,
    private val targetHost: String,
    private val targetPort: Int,
    private val protectSocket: (Socket) -> Boolean
  ) : Proxy {
    private lateinit var socket: Socket

    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) {
      val raw = Socket()
      protectSocket(raw)
      raw.connect(InetSocketAddress(proxyHost, proxyPort), timeout)

      val hostPort = "$targetHost:$targetPort"
      val payload = payloadTemplate
        .replace("[host_port]", hostPort)
        .replace("[host]", targetHost)
        .replace("[crlf]", "\r\n")

      raw.getOutputStream().apply {
        write(payload.toByteArray(Charsets.US_ASCII))
        flush()
      }

      val response = StringBuilder()
      val buf = ByteArray(1)
      var newlineCount = 0
      while (newlineCount < 2) {
        val n = raw.getInputStream().read(buf)
        if (n < 0) break
        response.append(buf[0].toInt().toChar())
        newlineCount = if (buf[0] == '\n'.code.toByte()) newlineCount + 1 else 0
      }
      if (!response.contains("200")) {
        raw.close()
        throw IOException("Proxy payload ditolak, response: ${response.toString().take(80)}")
      }

      socket = raw
    }

    override fun getInputStream(): InputStream = socket.getInputStream()
    override fun getOutputStream(): OutputStream = socket.getOutputStream()
    override fun getSocket(): Socket = socket
    override fun close() {
      try { socket.close() } catch (ignored: Exception) {}
    }
  }
}
