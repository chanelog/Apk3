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
import java.net.InetSocketAddress
import java.net.Socket

class HnTunnelVpnService : VpnService() {
  companion object { const val ACTION_START = "com.example.service.START"; const val ACTION_STOP = "com.example.service.STOP"; private const val NOTIFICATION_ID = 8844; private const val CHANNEL_ID = "hn_tunnel_vpn_channel"; private const val LOCAL_SOCKS_PORT = 10808 }
  private var vpnInterface: ParcelFileDescriptor? = null
  private var wakeLock: PowerManager.WakeLock? = null
  private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
  private var tunnelJob: Job? = null
  private var statsJob: Job? = null
  private var sshBridge: SshSocksBridge? = null

  override fun onCreate() { super.onCreate(); createNotificationChannel(); wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HnTunnel::WakeLock") }
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when (intent?.action) { ACTION_START -> startTunnel(TunnelController.activeConfig.value); ACTION_STOP -> stopTunnel() }; return START_NOT_STICKY }

  private fun startTunnel(config: TunnelConfig) {
    if (TunnelController.tunnelState.value == TunnelState.CONNECTED) return
    wakeLock?.acquire(24 * 60 * 60 * 1000L); startForeground(NOTIFICATION_ID, buildNotification("Connecting to ${config.name}...", "Initializing tunnel engine"))
    tunnelJob?.cancel()
    tunnelJob = serviceScope.launch {
      try {
        TunnelController.updateState(TunnelState.CONNECTING); LogManager.i("Starting HN Tunnel Engine..."); LogManager.i("Tunnel Mode: ${config.type.displayName}")
        val builder = Builder().setMtu(1500).addAddress("10.8.0.2", 32).setSession("HN Tunnel - ${config.name}")
        builder.addDnsServer(if (config.customDns1.isNotBlank()) config.customDns1 else "8.8.8.8"); builder.addDnsServer(if (config.customDns2.isNotBlank()) config.customDns2 else "1.1.1.1"); builder.addRoute("0.0.0.0", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setMetered(false)
        try { builder.addDisallowedApplication(packageName) } catch (e: Exception) { LogManager.w("Tidak bisa exclude package sendiri: ${e.message}") }
        val pfd = builder.establish() ?: throw IllegalStateException("Failed to establish VPN interface")
        vpnInterface = pfd; LogManager.s("Virtual interface tun0 established")
        TunnelController.updateState(TunnelState.HANDSHAKING)
        when (config.type) {
          TunnelType.SSH_DIRECT, TunnelType.SSH_SSL_TLS, TunnelType.SSH_HTTP_PROXY -> { connectSshBackend(config); if (!HevSocks5Bridge.start(this@HnTunnelVpnService, LOCAL_SOCKS_PORT, enableUdp = config.enableUdp, tunFd = pfd.fd)) throw IllegalStateException("tun2socks gagal start") }
          TunnelType.V2RAY_VMESS, TunnelType.V2RAY_VLESS, TunnelType.TROJAN -> connectXrayBackend(config, pfd.fd)
        }
        if (!verifySocksConnectivity()) throw IllegalStateException("Backend hidup tetapi tidak dapat meneruskan koneksi HTTPS")
        TunnelController.updateState(TunnelState.CONNECTED); LogManager.s("Tunnel connected: connectivity check berhasil"); updateNotification("Connected: ${config.name}", "HN Tunnel Active"); startStatsMonitor(config)
      } catch (e: Exception) { LogManager.e("Tunnel connection failed: ${e.message}"); TunnelController.updateState(TunnelState.DISCONNECTED); stopTunnel() }
    }
  }

  private suspend fun verifySocksConnectivity(): Boolean = withContext(Dispatchers.IO) {
    repeat(3) { attempt ->
      try {
        delay(if (attempt == 0) 150 else 350)
        Socket().use { socket ->
          socket.soTimeout = 8000; socket.connect(InetSocketAddress("127.0.0.1", LOCAL_SOCKS_PORT), 2000)
          val input = socket.getInputStream(); val output = socket.getOutputStream()
          output.write(byteArrayOf(5, 1, 0)); output.flush()
          if (input.read() != 5 || input.read() != 0) return@use
          output.write(byteArrayOf(5, 1, 0, 1, 1, 1, 1, 1, 0, 443)); output.flush()
          val reply = ByteArray(10); var offset = 0
          while (offset < reply.size) { val n = input.read(reply, offset, reply.size - offset); if (n < 0) return@use; offset += n }
          if (reply[0].toInt() == 5 && reply[1].toInt() == 0) { LogManager.s("Connectivity check: SOCKS -> 1.1.1.1:443 OK"); return@withContext true }
        }
      } catch (e: Exception) { LogManager.d("Connectivity check attempt ${attempt + 1}: ${e.message}") }
    }
    false
  }

  private suspend fun connectSshBackend(config: TunnelConfig) = withContext(Dispatchers.IO) { TunnelController.updateState(TunnelState.AUTHENTICATING); val bridge = SshSocksBridge(LOCAL_SOCKS_PORT); bridge.start(config) { socket -> protect(socket) }; sshBridge = bridge; LogManager.s("SSH SOCKS lokal aktif") }
  private suspend fun connectXrayBackend(config: TunnelConfig, tunFd: Int) = withContext(Dispatchers.IO) { TunnelController.updateState(TunnelState.AUTHENTICATING); XrayCoreBridge.start(com.example.tunnel.XrayConfigBuilder.build(config, LOCAL_SOCKS_PORT), tunFd, filesDir.absolutePath); if (!XrayCoreBridge.isRunning()) throw IllegalStateException("Xray tidak running") }

  private fun startStatsMonitor(config: TunnelConfig) { statsJob?.cancel(); statsJob = serviceScope.launch { var seconds = 0L; var lastRx = 0L; var lastTx = 0L; while (isActive && TunnelController.tunnelState.value == TunnelState.CONNECTED) { delay(1000); seconds++; val raw = HevSocks5Bridge.stats(); val tx = raw?.getOrNull(1) ?: lastTx; val rx = raw?.getOrNull(3) ?: lastRx; TunnelController.updateStats(TunnelStats(bytesIn = rx, bytesOut = tx, speedInBps = (rx - lastRx).coerceAtLeast(0) * 8, speedOutBps = (tx - lastTx).coerceAtLeast(0) * 8, durationSeconds = seconds)); lastRx = rx; lastTx = tx } } }
  private fun stopTunnel() { serviceScope.launch { TunnelController.updateState(TunnelState.STOPPING); statsJob?.cancel(); tunnelJob?.cancel(); try { HevSocks5Bridge.stop() } catch (_: Exception) {}; try { com.example.tunnel.XrayCoreBridge.stop() } catch (_: Exception) {}; try { sshBridge?.stop() } catch (_: Exception) {}; sshBridge = null; try { vpnInterface?.close() } catch (_: Exception) {}; vpnInterface = null; if (wakeLock?.isHeld == true) wakeLock?.release(); TunnelController.updateState(TunnelState.DISCONNECTED); TunnelController.updateStats(TunnelStats()); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() } }
  override fun onDestroy() { stopTunnel(); super.onDestroy() }
  private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java)?.createNotificationChannel(NotificationChannel(CHANNEL_ID, "HN Tunnel Service", NotificationManager.IMPORTANCE_LOW)) }
  private fun buildNotification(title: String, content: String): Notification { val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); val stop = PendingIntent.getService(this, 1, Intent(this, HnTunnelVpnService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT); return NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(title).setContentText(content).setContentIntent(open).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stop).setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build() }
  private fun updateNotification(title: String, content: String) { (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(NOTIFICATION_ID, buildNotification(title, content)) }
}
