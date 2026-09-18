package com.example.tunnel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.model.TunnelConfig
import com.example.model.TunnelState
import com.example.model.TunnelStats
import com.example.service.HnTunnelVpnService
import com.example.util.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TunnelController {

  private val _tunnelState = MutableStateFlow(TunnelState.DISCONNECTED)
  val tunnelState: StateFlow<TunnelState> = _tunnelState.asStateFlow()

  private val _tunnelStats = MutableStateFlow(TunnelStats())
  val tunnelStats: StateFlow<TunnelStats> = _tunnelStats.asStateFlow()

  private val _activeConfig = MutableStateFlow(ConfigManager.DEFAULT_PROFILES[1])
  val activeConfig: StateFlow<TunnelConfig> = _activeConfig.asStateFlow()

  fun init(context: Context) {
    _activeConfig.value = ConfigManager.getActiveConfig(context)
  }

  fun setConfig(context: Context, config: TunnelConfig) {
    _activeConfig.value = config
    ConfigManager.saveActiveConfig(context, config)
  }

  fun updateState(state: TunnelState) {
    _tunnelState.value = state
  }

  fun updateStats(stats: TunnelStats) {
    _tunnelStats.value = stats
  }

  fun isVpnPrepared(context: Context): Boolean {
    return VpnService.prepare(context) == null
  }

  fun startTunnel(context: Context, config: TunnelConfig = _activeConfig.value) {
    _activeConfig.value = config
    ConfigManager.saveActiveConfig(context, config)

    val intent = Intent(context, HnTunnelVpnService::class.java).apply {
      action = HnTunnelVpnService.ACTION_START
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }

  fun stopTunnel(context: Context) {
    val intent = Intent(context, HnTunnelVpnService::class.java).apply {
      action = HnTunnelVpnService.ACTION_STOP
    }
    context.startService(intent)
  }
}
