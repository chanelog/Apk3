package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.TunnelConfig
import com.example.model.TunnelState
import com.example.model.TunnelStats
import com.example.model.TunnelType
import com.example.tunnel.HevSocks5Bridge
import com.example.tunnel.SshSocksBridge
import com.example.tunnel.TunnelController
import com.example.tunnel.XrayConfigBuilder
import com.example.tunnel.XrayProcessBridge
import com.example.util.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class HnTunnelVpnService : VpnService() {
  companion object {
    const val ACTION_START = "com.example.service.START"
    const val ACTION_STOP = "com.example.service.STOP"
    private const val NOTIFICATION_ID = 8844
    private const val CHANNEL_ID = "hn_tunnel_vpn_channel"
    const val LOCAL_SOCKS_PORT = 10808
  }

  private var vpnInterface: ParcelFileDescriptor? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
  private var tunnelJob: Job? = null
  private var statsJob: Job? = null
  private var sshBridge: SshSocksBridge? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
      .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HnTunnel::WakeLock")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> startTunnel(TunnelController.activeConfig.value)
      ACTION_STOP -> stopTunnel()
    }
    return START_NOT_STICKY
  }

  private fun startTunnel(config: TunnelConfig) {
    if (TunnelController.tunnelState.value == TunnelState.CONNECTED) return
    wakeLock?.acquire(24 * 60 * 60 * 1000L)
    startForeground(NOTIFICATION_ID, buildNotification("Connecting to ${config.name}...", "Initializing tunnel engine"))
    tunnelJob?.cancel()
    tunnelJob = serviceScope.launch {
      try {
        TunnelController.updateState(TunnelState.CONNECTING)
        LogManager.i("Starting HN Tunnel Engine...")
        LogManager.i("Tunnel Mode: ${config.type.displayName}")

        val builder = Builder()
          .setMtu(1500)
          .addAddress("10.8.0.2", 32)
          .setSession("HN Tunnel - ${config.name}")
        builder.addDnsServer(if (config.customDns1.isNotBlank()) config.customDns1 else "8.8.8.8")
        builder.addDnsServer(if (config.customDns2.isNotBlank()) config.customDns2 else "1.1.1.1")
        builder.addRoute("0.0.0.0", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setMetered(false)
        try { builder.addDisallowedApplication(packageName) } catch (e: Exception) { LogManager.w("Tidak bisa exclude package sendiri: ${e.message}") }

        val pfd = builder.establish() ?: throw IllegalStateException("Failed to establish VPN interface")
        vpnInterface = pfd
        LogManager.s("Virtual interface tun0 established")
        startTunActivityMonitor(pfd)

        when (config.type) {
          TunnelType.SSH_DIRECT, TunnelType.SSH_SSL_TLS, TunnelType.SSH_HTTP_PROXY -> {
            connectSshBackend(config)
            if (!HevSocks5Bridge.start(this@HnTunnelVpnService, LOCAL_SOCKS_PORT, enableUdp = config.enableUdp, tunFd = pfd.fd)) {
              throw IllegalStateException("tun2socks gagal start")
            }
          }
          TunnelType.V2RAY_VMESS, TunnelType.V2RAY_VLESS, TunnelType.TROJAN -> {
            val xrayJson = XrayConfigBuilder.build(config, LOCAL_SOCKS_PORT)
            val started = HevSocks5Bridge.start(this@HnTunnelVpnService, LOCAL_SOCKS_PORT, enableUdp = config.enableUdp, tunFd = pfd.fd)
            if (!started) {
              throw IllegalStateException("HevSocks5Bridge gagal start untuk Xray")
            }
            if (!XrayProcessBridge.start(this@HnTunnelVpnService, xrayJson, LOCAL_SOCKS_PORT)) {
              throw IllegalStateException("Xray native process gagal start")
            }
            LogManager.s("Xray berjalan sebagai SOCKS lokal di 127.0.0.1:$LOCAL_SOCKS_PORT")
          }
        }

        if (!verifySocksConnectivity()) {
          throw IllegalStateException("SOCKS local tidak merespons / trafik tidak bisa terhubung")
        }

        TunnelController.updateState(TunnelState.CONNECTED)
        LogManager.s("Tunnel connected: connectivity check berhasil")
        updateNotification("Connected: ${config.name}", "HN Tunnel Active")
        startStatsMonitor(config)
      } catch (e: Exception) {
        LogManager.e("Tunnel connection failed: ${e.message}")
        TunnelController.updateState(TunnelState.DISCONNECTED)
        stopTunnel()
      }
    }
  }

  private suspend fun verifySocksConnectivity(): Boolean = withContext(Dispatchers.IO) {
    repeat(3) { attempt ->
      try {
        delay(if (attempt == 0) 150 else 350)
        Socket().use { socket ->
          socket.soTimeout = 8000
          socket.connect(InetSocketAddress("127.0.0.1", LOCAL_SOCKS_PORT), 2000)
          val input = socket.getInputStream()
          val output = socket.getOutputStream()
          output.write(byteArrayOf(5.toByte(), 1.toByte(), 0.toByte()))
          output.flush()
          if (input.read() != 5 || input.read() != 0) return@use

          val req = byteArrayOf(
            5.toByte(), 1.toByte(), 0.toByte(), 1.toByte(),
            0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(),
            0.toByte(), 443.toByte()
          )
          output.write(req)
          output.flush()

          val reply = ByteArray(10)
          var offset = 0
          while (offset < reply.size) {
            val n = input.read(reply, offset, reply.size - offset)
            if (n < 0) return@use
            offset += n
          }

          if (reply[0].toInt() == 5 && reply[1].toInt() == 0) {
            LogManager.s("Connectivity check: SOCKS -> 127.0.0.1:$LOCAL_SOCKS_PORT OK")
            return@withContext true
          }
        }
      } catch (e: Exception) {
        LogManager.d("Connectivity check attempt ${attempt + 1}: ${e.message}")
      }
    }
    false
  }

  private suspend fun connectSshBackend(config: TunnelConfig) = withContext(Dispatchers.IO) {
    LogManager.i("Membuka koneksi SSH (${config.type.displayName}) ke ${config.sshHost}:${config.sshPort}...")
    TunnelController.updateState(TunnelState.AUTHENTICATING)
    val bridge = SshSocksBridge(LOCAL_SOCKS_PORT)
    bridge.start(config) { socket -> protect(socket) }
    sshBridge = bridge
    LogManager.s("SSH SOCKS lokal aktif")
  }

  private fun startStatsMonitor(config: TunnelConfig) {
    statsJob?.cancel()
    statsJob = serviceScope.launch {
      var seconds = 0L
      var lastRx = 0L
      var lastTx = 0L
      while (isActive && TunnelController.tunnelState.value == TunnelState.CONNECTED) {
        delay(1000)
        seconds++
        val raw = HevSocks5Bridge.stats()
        val tx = raw?.getOrNull(1) ?: lastTx
        val rx = raw?.getOrNull(3) ?: lastRx
        TunnelController.updateStats(
          TunnelStats(
            bytesIn = rx,
            bytesOut = tx,
            speedInBps = (rx - lastRx).coerceAtLeast(0) * 8,
            speedOutBps = (tx - lastTx).coerceAtLeast(0) * 8,
            durationSeconds = seconds
          )
        )
        lastRx = rx
        lastTx = tx
      }
    }
  }

  private fun stopTunnel() {
    serviceScope.launch {
      TunnelController.updateState(TunnelState.STOPPING)
      statsJob?.cancel()
      tunnelJob?.cancel()

      try { HevSocks5Bridge.stop() } catch (_: Exception) {}
      try { XrayProcessBridge.stop() } catch (_: Exception) {}
      try { sshBridge?.stop() } catch (_: Exception) {}
      sshBridge = null
      try { vpnInterface?.close() } catch (_: Exception) {}
      vpnInterface = null

      if (wakeLock?.isHeld == true) wakeLock?.release()
      TunnelController.updateState(TunnelState.DISCONNECTED)
      TunnelController.updateStats(TunnelStats())
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  override fun onDestroy() {
    stopTunnel()
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      getSystemService(NotificationManager::class.java)?.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "HN Tunnel Service", NotificationManager.IMPORTANCE_LOW)
      )
    }
  }

  private fun buildNotification(title: String, content: String): Notification {
    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val stop = PendingIntent.getService(this, 1, Intent(this, HnTunnelVpnService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(content)
      .setContentIntent(open)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stop)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun updateNotification(title: String, content: String) {
    (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(NOTIFICATION_ID, buildNotification(title, content))
  }

  private fun startTunActivityMonitor(pfd: ParcelFileDescriptor) {
    serviceScope.launch(Dispatchers.IO) {
      var totalHits = 0
      var windowHits = 0
      val startTime = System.currentTimeMillis()
      var lastLogTime = startTime
      try {
        val fd = pfd.fileDescriptor
        while (isActive && vpnInterface != null) {
          val pollfd = android.system.StructPollfd().apply {
            this.fd = fd
            this.events = android.system.OsConstants.POLLIN.toShort()
          }
          val n = try {
            android.system.Os.poll(arrayOf(pollfd), 200)
          } catch (e: Exception) {
            LogManager.d("DIAGNOSTIK: poll() TUN error: ${e.message}")
            break
          }
          if (n > 0 && (pollfd.revents.toInt() and android.system.OsConstants.POLLIN) != 0) {
            windowHits++
            totalHits++
          }

          val now = System.currentTimeMillis()
          if (now - lastLogTime >= 5000) {
            val elapsedSec = (now - startTime) / 1000
            if (totalHits == 0) {
              LogManager.e("DIAGNOSTIK: TUN belum menerima paket APAPUN dalam ${elapsedSec}s.")
            } else {
              LogManager.d("DIAGNOSTIK: TUN aktif menerima paket ($windowHits sampel dalam 5s terakhir, total $totalHits sample / ${elapsedSec}s).")
            }
            windowHits = 0
            lastLogTime = now
          }
          delay(300)
        }
      } catch (e: Exception) {
        LogManager.d("DIAGNOSTIK: monitor TUN berhenti: ${e.message}")
      }
    }
  }
}
