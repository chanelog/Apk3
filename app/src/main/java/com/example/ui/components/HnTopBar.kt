package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HnTopBar(
  state: TunnelState,
  onImportClick: () -> Unit,
  onExportClick: () -> Unit,
  onLogsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onResetDefaultsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(ObsidianDark)
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HnLogoBadge(size = 38.dp)

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "HN Tunnel",
          color = TextPrimary,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Connection State Pill
        val statusColor = when (state) {
          TunnelState.CONNECTED -> EmeraldNeon
          TunnelState.CONNECTING, TunnelState.AUTHENTICATING, TunnelState.HANDSHAKING -> CyanNeon
          TunnelState.STOPPING -> RubyNeon
          TunnelState.DISCONNECTED -> TextSecondary
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .border(0.6.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = when (state) {
              TunnelState.CONNECTED -> "ON"
              TunnelState.DISCONNECTED -> "OFF"
              else -> "SYNC"
            },
            color = statusColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
          )
        }
      }

      Text(
        text = "SSH • V2Ray • SSL/TLS",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
    }

    // Direct Action: Settings (Auto Ping, DNS, etc.)
    IconButton(
      onClick = onSettingsClick,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = EmeraldNeon,
        modifier = Modifier.size(20.dp)
      )
    }

    // Direct Action: Quick Logs
    IconButton(
      onClick = onLogsClick,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Terminal,
        contentDescription = "Logs",
        tint = CyanNeon,
        modifier = Modifier.size(20.dp)
      )
    }

    // Overflow menu
    Box {
      IconButton(
        onClick = { menuExpanded = true },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "Options",
          tint = TextPrimary,
          modifier = Modifier.size(20.dp)
        )
      }

      DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        modifier = Modifier
          .background(Color(0xFF0F172A))
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      ) {
        DropdownMenuItem(
          text = { Text("Settings (Auto Ping & Tunnel)", color = TextPrimary) },
          leadingIcon = {
            Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldNeon)
          },
          onClick = {
            menuExpanded = false
            onSettingsClick()
          }
        )
        DropdownMenuItem(
          text = { Text("Import Config (.hnt / vmess)", color = TextPrimary) },
          leadingIcon = {
            Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldNeon)
          },
          onClick = {
            menuExpanded = false
            onImportClick()
          }
        )
        DropdownMenuItem(
          text = { Text("Export Config (.hnt)", color = TextPrimary) },
          leadingIcon = {
            Icon(Icons.Default.FileUpload, contentDescription = null, tint = CyanNeon)
          },
          onClick = {
            menuExpanded = false
            onExportClick()
          }
        )
        DropdownMenuItem(
          text = { Text("Reset to Preset Configs", color = TextSecondary) },
          onClick = {
            menuExpanded = false
            onResetDefaultsClick()
          }
        )
      }
    }
  }
}
