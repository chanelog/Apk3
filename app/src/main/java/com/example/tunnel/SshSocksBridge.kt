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

/** SSH session plus a local SOCKS5 server used by tun2socks. */
class SshSocksBridge(private val localSocksPort: Int) {
  private var session: Session? = null
  private var serverSocket: ServerSocket? = null
  private val scope = CoroutineScope(Dispatchers.IO + Job())
  private var acceptJob: Job? = null

  @Volatile var isConnected: Boolean = false
    private set

  @Throws(Exception::class)
  fun start(config: TunnelConfig, protectSocket: (Socket) -> Boolean) {
    stop()
    val jsch = JSch()
    val newSession = jsch.getSession(config.sshUsername, config.sshHost, config.sshPort)
    newSession.setConfig("StrictHostKeyChecking", "no")
    newSession.setPassword(config.sshPassword)
    newSession.timeout = 15000

    when (config.type) {
      TunnelType.SSH_SSL_TLS -> newSession.setProxy(
        TlsSniProxy(config.sshHost, config.sshPort, config.sniHost, protectSocket)
      )
      TunnelType.SSH_HTTP_PROXY -> {
        val target = parseHostPort(config.remoteProxy, config.sshHost, config.sshPort)
        newSession.setProxy(
          PayloadInjectProxy(
            target.first, target.second, config.payload,
            config.sshHost, config.sshPort, protectSocket
          )
        )
      }
      TunnelType.SSH_DIRECT -> newSession.setProxy(
        DirectProxy(config.sshHost, config.sshPort, protectSocket)
      )
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
        val client = try { server.accept() } catch (_: IOException) { break }
        launch { handleSocksClient(client) }
      }
    }
  }

  private suspend fun handleSocksClient(client: Socket) {
    try {
      client.tcpNoDelay = true
      val input = client.getInputStream()
      val output = client.getOutputStream()
      if (input.read() != 0x05) { client.close(); return }
      val nMethods = input.read()
      if (nMethods < 0) { client.close(); return }
      readFully(input, ByteArray(nMethods))
      output.write(byteArrayOf(0x05, 0x00)); output.flush()

      val reqVer = input.read()
      val cmd = input.read()
      input.read()
      val atyp = input.read()
      if (reqVer != 0x05 || cmd != 0x01) {
        output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0)); client.close(); return
      }
      val targetHost = when (atyp) {
        0x01 -> { val a = ByteArray(4); readFully(input, a); a.joinToString(".") { (it.toInt() and 255).toString() } }
        0x03 -> { val len = input.read(); if (len < 0) throw IOException("Invalid SOCKS domain length"); val d = ByteArray(len); readFully(input, d); String(d, Charsets.US_ASCII) }
        0x04 -> { val a = ByteArray(16); readFully(input, a); java.net.InetAddress.getByAddress(a).hostAddress ?: "" }
        else -> { client.close(); return }
      }
      val portBytes = ByteArray(2); readFully(input, portBytes)
      val targetPort = ((portBytes[0].toInt() and 255) shl 8) or (portBytes[1].toInt() and 255)
      val activeSession = session
      if (activeSession == null || !activeSession.isConnected) {
        output.write(byteArrayOf(0x05, 0x01, 0x00, 0x01, 0, 0, 0, 0, 0, 0)); client.close(); return
      }
      val channel = activeSession.openChannel("direct-tcpip") as ChannelDirectTCPIP
      channel.setHost(targetHost); channel.setPort(targetPort); channel.connect(10000)
      output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0)); output.flush()
      pipe(client, channel)
    } catch (e: Exception) {
      LogManager.d("SOCKS session error: ${e.message}")
      try { client.close() } catch (_: Exception) {}
    }
  }

  private suspend fun pipe(client: Socket, channel: Channel) {
    val toChannel = scope.launch { try { client.getInputStream().copyTo(channel.outputStream, 16384) } catch (_: Exception) {} }
    val toClient = scope.launch { try { channel.inputStream.copyTo(client.getOutputStream(), 16384) } catch (_: Exception) {} }
    toChannel.join(); toClient.join()
    try { channel.disconnect() } catch (_: Exception) {}
    try { client.close() } catch (_: Exception) {}
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
    val idx = raw.lastIndexOf(':')
    return if (idx > 0) raw.substring(0, idx) to (raw.substring(idx + 1).toIntOrNull() ?: fallbackPort) else raw to fallbackPort
  }

  fun stop() {
    isConnected = false; acceptJob?.cancel()
    try { serverSocket?.close() } catch (_: Exception) {}
    try { session?.disconnect() } catch (_: Exception) {}
    serverSocket = null; session = null
  }

  private class DirectProxy(private val connectHost: String, private val connectPort: Int, private val protectSocket: (Socket) -> Boolean) : Proxy {
    private lateinit var socket: Socket
    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) { socket = Socket().also { protectSocket(it); it.connect(InetSocketAddress(connectHost, connectPort), timeout) } }
    override fun getInputStream() = socket.getInputStream()
    override fun getOutputStream() = socket.getOutputStream()
    override fun getSocket() = socket
    override fun close() { try { socket.close() } catch (_: Exception) {} }
  }

  private class TlsSniProxy(private val connectHost: String, private val connectPort: Int, private val sniHost: String, private val protectSocket: (Socket) -> Boolean) : Proxy {
    private lateinit var socket: Socket
    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) {
      val raw = Socket(); protectSocket(raw); raw.connect(InetSocketAddress(connectHost, connectPort), timeout)
      val trustAll = object : X509TrustManager { override fun checkClientTrusted(c: Array<out X509Certificate>?, a: String?) {}; override fun checkServerTrusted(c: Array<out X509Certificate>?, a: String?) {}; override fun getAcceptedIssuers() = arrayOf<X509Certificate>() }
      val context = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustAll), SecureRandom()) }
      val ssl = context.socketFactory.createSocket(raw, sniHost, connectPort, true) as SSLSocket
      ssl.sslParameters = ssl.sslParameters.apply { serverNames = listOf(SNIHostName(sniHost)) }
      ssl.startHandshake(); socket = ssl
    }
    override fun getInputStream() = socket.getInputStream(); override fun getOutputStream() = socket.getOutputStream(); override fun getSocket() = socket
    override fun close() { try { socket.close() } catch (_: Exception) {} }
  }

  private class PayloadInjectProxy(private val proxyHost: String, private val proxyPort: Int, private val payloadTemplate: String, private val targetHost: String, private val targetPort: Int, private val protectSocket: (Socket) -> Boolean) : Proxy {
    private lateinit var socket: Socket
    override fun connect(socket_factory: SocketFactory?, host: String?, port: Int, timeout: Int) {
      val raw = Socket(); protectSocket(raw); raw.connect(InetSocketAddress(proxyHost, proxyPort), timeout)
      val payload = payloadTemplate.replace("[host_port]", "$targetHost:$targetPort").replace("[host]", targetHost).replace("[crlf]", "\r\n")
      raw.getOutputStream().apply { write(payload.toByteArray(Charsets.US_ASCII)); flush() }
      val response = StringBuilder(); val buf = ByteArray(1); var newlineCount = 0
      while (newlineCount < 2) { val n = raw.getInputStream().read(buf); if (n < 0) break; response.append(buf[0].toInt().toChar()); newlineCount = if (buf[0] == '\n'.code.toByte()) newlineCount + 1 else 0 }
      // HTTP CONNECT proxies return 200; WebSocket upgrades return 101.
      if (!Regex("HTTP/\\d(?:\\.\\d)?\\s+(?:200|101)\\b").containsMatchIn(response)) { raw.close(); throw IOException("Proxy handshake ditolak: ${response.toString().take(120)}") }
      socket = raw
    }
    override fun getInputStream() = socket.getInputStream(); override fun getOutputStream() = socket.getOutputStream(); override fun getSocket() = socket
    override fun close() { try { socket.close() } catch (_: Exception) {} }
  }
}
