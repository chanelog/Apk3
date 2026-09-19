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
import com.example.util.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HnTunnelVpnService : VpnService() {

  companion object {
    const val ACTION_START = "com.example.service.START"
    const val ACTION_STOP = "com.example.service.STOP"
    private const val NOTIFICATION_ID = 8844
    private const val CHANNEL_ID = "hn_tunnel_vpn_channel"
    private const val LOCAL_SOCKS_PORT = 10808
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
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HnTunnel::WakeLock")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        val config = TunnelController.activeConfig.value
        startTunnel(config)
      }
      ACTION_STOP -> {
        stopTunnel()
      }
    }
    return START_NOT_STICKY
  }

  private fun startTunnel(config: TunnelConfig) {
    if (TunnelController.tunnelState.value == TunnelState.CONNECTED) {
      return
    }

    wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 hours max
    val initialNotification = buildNotification("Connecting to ${config.name}...", "Initializing tunnel engine")
    startForeground(NOTIFICATION_ID, initialNotification)

    tunnelJob?.cancel()
    tunnelJob = serviceScope.launch {
      try {
        TunnelController.updateState(TunnelState.CONNECTING)
        LogManager.i("Starting HN Tunnel Engine...")
        LogManager.i("Tunnel Mode: ${config.type.displayName}")
        LogManager.i("Profile: ${config.name}")

        // 1. Sambungkan backend (SSH atau, nanti, Xray) SEBELUM TUN interface
        //    dibuka, supaya kalau backend gagal connect kita belum terlanjur
        //    mem-blackhole semua trafik user.
        TunnelController.updateState(TunnelState.HANDSHAKING)
        when (config.type) {
          TunnelType.SSH_DIRECT, TunnelType.SSH_SSL_TLS, TunnelType.SSH_HTTP_PROXY ->
            connectSshBackend(config)
          TunnelType.V2RAY_VMESS, TunnelType.V2RAY_VLESS, TunnelType.TROJAN ->
            connectXrayBackend(config)
        }

        // 2. Baru sekarang buka TUN interface
        LogManager.d("Allocating virtual network interface (TUN)...")
        val builder = Builder()
          .setMtu(1500)
          .addAddress("10.8.0.2", 32)
          .setSession("HN Tunnel - ${config.name}")

        val primaryDns = if (config.customDns1.isNotBlank()) config.customDns1 else "8.8.8.8"
        val secondaryDns = if (config.customDns2.isNotBlank()) config.customDns2 else "1.1.1.1"
        builder.addDnsServer(primaryDns)
        builder.addDnsServer(secondaryDns)

        // Baik DIRECT_TUNNEL maupun GLOBAL_TUN sama-sama route semua trafik ke TUN,
        // karena penyaringan "internet normal tetap jalan" sekarang dilakukan oleh
        // tun2socks + backend proxy, bukan oleh routing table VPN lagi.
        builder.addRoute("0.0.0.0", 0)
        LogManager.i("Routing mode: ${config.routingMode}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          builder.setMetered(false)
        }
        // Cegah aplikasi ini sendiri ikut terjebak lewat TUN (hindari routing loop)
        try {
          builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
          LogManager.w("Tidak bisa exclude package sendiri: ${e.message}")
        }

        val pfd = builder.establish()
        if (pfd == null) {
          LogManager.e("Failed to establish VPN interface. Permission might be revoked.")
          stopTunnel()
          return@launch
        }
        vpnInterface = pfd
        LogManager.s("Virtual interface tun0 established (MTU 1500).")

        // 3. Jalankan tun2socks: jembatani TUN fd <-> SOCKS5 lokal (backend di atas)
        val started = HevSocks5Bridge.start(
          context = this@HnTunnelVpnService,
          socksPort = LOCAL_SOCKS_PORT,
          enableUdp = config.enableUdp,
          tunFd = pfd.fd
        )
        if (!started) {
          LogManager.e("tun2socks gagal start. Cek logcat filter TProxyService/hev-socks5-tunnel.")
          stopTunnel()
          return@launch
        }
        LogManager.s("tun2socks aktif, trafik TUN diarahkan ke SOCKS5 127.0.0.1:$LOCAL_SOCKS_PORT")

        TunnelController.updateState(TunnelState.CONNECTED)
        LogManager.s("Tunnel successfully connected! Internet data bridge is ACTIVE.")
        updateNotification("Connected: ${config.name}", "HN Tunnel Active")

        startStatsMonitor(config)

      } catch (e: Exception) {
        LogManager.e("Tunnel connection failed: ${e.message}")
        TunnelController.updateState(TunnelState.DISCONNECTED)
        stopTunnel()
      }
    }
  }

  private suspend fun connectSshBackend(config: TunnelConfig) = withContext(Dispatchers.IO) {
    LogManager.i("Membuka koneksi SSH (${config.type.displayName}) ke ${config.sshHost}:${config.sshPort}...")
    TunnelController.updateState(TunnelState.AUTHENTICATING)
    val bridge = SshSocksBridge(LOCAL_SOCKS_PORT)
    bridge.start(config) // melempar exception kalau gagal -> ditangkap di startTunnel()
    sshBridge = bridge
    LogManager.s("SSH tersambung, SOCKS5 lokal dibuka di 127.0.0.1:$LOCAL_SOCKS_PORT")
  }

  private suspend fun connectXrayBackend(config: TunnelConfig) = withContext(Dispatchers.IO) {
    LogManager.i("Menyiapkan Xray-core (${config.type.displayName})...")
    TunnelController.updateState(TunnelState.AUTHENTICATING)
    val json = com.example.tunnel.XrayConfigBuilder.build(config, LOCAL_SOCKS_PORT)
    com.example.tunnel.XrayCoreBridge.start(json) { fd -> protect(fd) }
    LogManager.s("Xray-core tersambung, SOCKS5 lokal dibuka di 127.0.0.1:$LOCAL_SOCKS_PORT")
  }

  private fun startStatsMonitor(config: TunnelConfig) {
    statsJob?.cancel()
    statsJob = serviceScope.launch {
      var seconds = 0L
      var lastRxBytes = 0L
      var lastTxBytes = 0L
      while (isActive && TunnelController.tunnelState.value == TunnelState.CONNECTED) {
        delay(1000)
        seconds++

        // [txPackets, txBytes, rxPackets, rxBytes] — lihat TProxyGetStats() di README resmi
        val raw = HevSocks5Bridge.stats()
        val txBytes = raw?.getOrNull(1) ?: lastTxBytes
        val rxBytes = raw?.getOrNull(3) ?: lastRxBytes
        val speedOut = (txBytes - lastTxBytes).coerceAtLeast(0)
        val speedIn = (rxBytes - lastRxBytes).coerceAtLeast(0)
        lastTxBytes = txBytes
        lastRxBytes = rxBytes

        val stats = TunnelStats(
          bytesIn = rxBytes,
          bytesOut = txBytes,
          speedInBps = speedIn * 8,
          speedOutBps = speedOut * 8,
          pingMs = TunnelController.tunnelStats.value.pingMs,
          durationSeconds = seconds
        )
        TunnelController.updateStats(stats)
        if (seconds % 5 == 0L) {
          updateNotification("HN Tunnel • ${stats.formatDuration()}", "Connected: ${config.name}")
        }
      }
    }
  }

  private fun stopTunnel() {
    serviceScope.launch {
      TunnelController.updateState(TunnelState.STOPPING)
      LogManager.i("Disconnecting HN Tunnel...")

      statsJob?.cancel()
      tunnelJob?.cancel()

      try { HevSocks5Bridge.stop() } catch (ignored: Exception) {}
      try { com.example.tunnel.XrayCoreBridge.stop() } catch (ignored: Exception) {}
      try { sshBridge?.stop() } catch (ignored: Exception) {}
      sshBridge = null

      try { vpnInterface?.close() } catch (e: Exception) {}
      vpnInterface = null

      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }

      delay(300)
      TunnelController.updateState(TunnelState.DISCONNECTED)
      TunnelController.updateStats(TunnelStats())
      LogManager.i("Tunnel disconnected. VPN interface closed.")

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
      val channel = NotificationChannel(
        CHANNEL_ID,
        "HN Tunnel Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Displays active tunnel connection status and speed"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(title: String, content: String): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingOpenIntent = PendingIntent.getActivity(
      this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val stopIntent = Intent(this, HnTunnelVpnService::class.java).apply {
      action = ACTION_STOP
    }
    val pendingStopIntent = PendingIntent.getService(
      this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(content)
      .setContentIntent(pendingOpenIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingStopIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun updateNotification(title: String, content: String) {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
  }
}
