package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TunnelConfig
import com.example.model.TunnelState
import com.example.tunnel.TunnelController
import com.example.ui.components.HnTopBar
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ConfigsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.PayloadGenScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.HnTunnelTheme
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ConfigManager
import com.example.util.LogManager

class MainActivity : ComponentActivity() {

  private val vpnPrepareLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      TunnelController.startTunnel(this)
    } else {
      Toast.makeText(this, "VPN permission is required to create tunnel", Toast.LENGTH_SHORT).show()
    }
  }

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { /* Handled */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    TunnelController.init(this)

    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    setContent {
      HnTunnelTheme(darkTheme = true) {
        HnTunnelApp(
          onToggleConnect = {
            val currentState = TunnelController.tunnelState.value
            if (currentState == TunnelState.CONNECTED ||
              currentState == TunnelState.CONNECTING ||
              currentState == TunnelState.AUTHENTICATING ||
              currentState == TunnelState.HANDSHAKING
            ) {
              TunnelController.stopTunnel(this)
            } else {
              val prepareIntent = VpnService.prepare(this)
              if (prepareIntent != null) {
                vpnPrepareLauncher.launch(prepareIntent)
              } else {
                TunnelController.startTunnel(this)
              }
            }
          }
        )
      }
    }
  }
}

enum class NavigationTab(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
  HOME("Tunnel", Icons.Filled.Security, Icons.Outlined.Security),
  PAYLOAD("Payload", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
  CONFIGS("Configs", Icons.Filled.Folder, Icons.Outlined.Folder),
  LOGS("Logs", Icons.Filled.Terminal, Icons.Outlined.Terminal),
  TOOLS("Tools", Icons.Filled.Build, Icons.Outlined.Build)
}

@Composable
fun HnTunnelApp(
  onToggleConnect: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

  val tunnelState by TunnelController.tunnelState.collectAsStateWithLifecycle()
  val tunnelStats by TunnelController.tunnelStats.collectAsStateWithLifecycle()
  val activeConfig by TunnelController.activeConfig.collectAsStateWithLifecycle()
  val logs by LogManager.logs.collectAsStateWithLifecycle()
  var showSettingsDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      HnTopBar(
        state = tunnelState,
        onImportClick = { currentTab = NavigationTab.CONFIGS },
        onExportClick = { currentTab = NavigationTab.CONFIGS },
        onLogsClick = { currentTab = NavigationTab.LOGS },
        onSettingsClick = { showSettingsDialog = true },
        onResetDefaultsClick = {
          TunnelController.setConfig(context, ConfigManager.DEFAULT_PROFILES[1])
          ConfigManager.saveProfiles(context, ConfigManager.DEFAULT_PROFILES)
          Toast.makeText(context, "Reset to default profiles", Toast.LENGTH_SHORT).show()
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = ObsidianDark,
        tonalElevation = 0.dp,
        modifier = Modifier
          .background(ObsidianDark)
          .navigationBarsPadding(),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
      ) {
        NavigationTab.values().forEach { tab ->
          val selected = currentTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                contentDescription = tab.label
              )
            },
            label = {
              Text(
                text = tab.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = ObsidianDark,
              selectedTextColor = EmeraldNeon,
              indicatorColor = EmeraldNeon,
              unselectedIconColor = TextSecondary,
              unselectedTextColor = TextSecondary
            )
          )
        }
      }
    },
    containerColor = ObsidianDark
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Crossfade(targetState = currentTab, label = "tab_fade") { tab ->
        when (tab) {
          NavigationTab.HOME -> {
            HomeScreen(
              config = activeConfig,
              state = tunnelState,
              stats = tunnelStats,
              onToggleConnect = onToggleConnect,
              onConfigChange = { updated ->
                TunnelController.setConfig(context, updated)
              },
              onOpenPayloadGen = {
                currentTab = NavigationTab.PAYLOAD
              }
            )
          }

          NavigationTab.PAYLOAD -> {
            PayloadGenScreen(
              currentPayload = activeConfig.payload,
              onApplyPayload = { newPayload ->
                TunnelController.setConfig(context, activeConfig.copy(payload = newPayload))
                currentTab = NavigationTab.HOME
              }
            )
          }

          NavigationTab.CONFIGS -> {
            ConfigsScreen(
              activeConfig = activeConfig,
              onSelectConfig = { selected ->
                TunnelController.setConfig(context, selected)
                currentTab = NavigationTab.HOME
              },
              onImportNewConfig = { imported ->
                TunnelController.setConfig(context, imported)
                currentTab = NavigationTab.HOME
              }
            )
          }

          NavigationTab.LOGS -> {
            LogsScreen(
              logs = logs,
              onClearLogs = {
                LogManager.clear()
              }
            )
          }

          NavigationTab.TOOLS -> {
            ToolsScreen(
              config = activeConfig,
              onUpdateDns = { provider, d1, d2 ->
                TunnelController.setConfig(
                  context,
                  activeConfig.copy(dnsProvider = provider, customDns1 = d1, customDns2 = d2)
                )
                Toast.makeText(context, "DNS updated to $provider", Toast.LENGTH_SHORT).show()
              },
              onUpdateAutoPing = { enabled, host, interval ->
                TunnelController.setConfig(
                  context,
                  activeConfig.copy(
                    autoPingEnabled = enabled,
                    autoPingHost = host,
                    autoPingIntervalSec = interval
                  )
                )
              }
            )
          }
        }
      }
    }
  }

  // Settings Dialog (Auto Ping, UDP, Reconnect, DNS)
  if (showSettingsDialog) {
    SettingsDialog(
      config = activeConfig,
      onDismiss = { showSettingsDialog = false },
      onSave = { updated ->
        TunnelController.setConfig(context, updated)
        showSettingsDialog = false
      }
    )
  }
}
