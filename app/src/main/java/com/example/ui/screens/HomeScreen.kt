package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelConfig
import com.example.model.TunnelState
import com.example.model.TunnelStats
import com.example.model.TunnelType
import com.example.ui.components.ConnectButton
import com.example.ui.components.TrafficMeter
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
  config: TunnelConfig,
  state: TunnelState,
  stats: TunnelStats,
  onToggleConnect: () -> Unit,
  onConfigChange: (TunnelConfig) -> Unit,
  onOpenPayloadGen: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  var showEditDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp)
      .padding(bottom = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Protocol Mode Horizontal Selector
    Text(
      text = "SELECT TUNNEL PROTOCOL",
      color = TextSecondary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.2.sp,
      modifier = Modifier
        .align(Alignment.Start)
        .padding(top = 8.dp, bottom = 6.dp)
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      TunnelType.values().forEach { type ->
        val selected = config.type == type
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EmeraldNeon.copy(alpha = 0.15f) else ObsidianCard)
            .border(
              width = if (selected) 1.2.dp else 0.8.dp,
              color = if (selected) EmeraldNeon else ObsidianBorder,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = state == TunnelState.DISCONNECTED) {
              onConfigChange(config.copy(type = type))
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = type.displayName,
            color = if (selected) EmeraldNeon else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Active Profile Summary Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = EmeraldNeon,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = config.name,
              color = TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = {
              if (config.isLocked && config.lockSsh) {
                Toast.makeText(context, "This config profile is locked by creator.", Toast.LENGTH_SHORT).show()
              } else {
                showEditDialog = true
              }
            },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = if (config.isLocked) Icons.Default.Lock else Icons.Default.Edit,
              contentDescription = "Edit Config",
              tint = if (config.isLocked) TextMuted else CyanNeon,
              modifier = Modifier.size(17.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic parameter badges depending on tunnel type
        when (config.type) {
          TunnelType.SSH_DIRECT, TunnelType.SSH_SSL_TLS, TunnelType.SSH_HTTP_PROXY -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ConfigBadge(label = "HOST", value = "${config.sshHost}:${config.sshPort}", modifier = Modifier.weight(1.2f))
              ConfigBadge(
                label = if (config.type == TunnelType.SSH_SSL_TLS) "SNI" else "USER",
                value = if (config.type == TunnelType.SSH_SSL_TLS) config.sniHost else config.sshUsername,
                modifier = Modifier.weight(1f)
              )
            }
          }
          TunnelType.V2RAY_VMESS, TunnelType.V2RAY_VLESS, TunnelType.TROJAN -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ConfigBadge(label = "SERVER", value = "${config.v2rayAddress}:${config.v2rayPort}", modifier = Modifier.weight(1.2f))
              ConfigBadge(label = "NET/SNI", value = "${config.v2rayNetwork} / ${config.v2raySni}", modifier = Modifier.weight(1f))
            }
          }
        }

        if (config.noteMessage.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Note: ${config.noteMessage}",
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 2
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 3. Main Glowing Tactile Connect Button
    ConnectButton(
      state = state,
      onClick = onToggleConnect,
      modifier = Modifier.padding(vertical = 6.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Real-time Traffic Meter
    TrafficMeter(
      stats = stats,
      isConnected = state == TunnelState.CONNECTED
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 5. Quick Tunnel Controls & Toggles
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        // Payload Quick Shortcut
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPayloadGen() }
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("HTTP Payload Generator", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Build custom injection headers & bugs", color = TextSecondary, fontSize = 10.sp)
            }
          }
          Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(ObsidianBorder))

        // UDP Forwarding Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Router, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Enable UDP Forwarding", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Required for games & voice calls", color = TextSecondary, fontSize = 10.sp)
            }
          }
          Switch(
            checked = config.enableUdp,
            onCheckedChange = { onConfigChange(config.copy(enableUdp = it)) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = EmeraldNeon,
              uncheckedTrackColor = ObsidianSurface
            )
          )
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(ObsidianBorder))

        // Auto Reconnect Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Auto Reconnect on Network Drop", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Persistent background daemon", color = TextSecondary, fontSize = 10.sp)
            }
          }
          Switch(
            checked = config.autoReconnect,
            onCheckedChange = { onConfigChange(config.copy(autoReconnect = it)) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = CyanNeon,
              uncheckedTrackColor = ObsidianSurface
            )
          )
        }
      }
    }
  }

  // Edit Configuration Dialog
  if (showEditDialog) {
    if (config.type == TunnelType.V2RAY_VMESS || config.type == TunnelType.V2RAY_VLESS || config.type == TunnelType.TROJAN) {
      XrayConfigEditorScreen(
        initialConfig = config,
        onSave = { updated ->
          onConfigChange(updated)
          showEditDialog = false
        },
        onDelete = {
          showEditDialog = false
        },
        onBack = {
          showEditDialog = false
        }
      )
      return
    }

    EditConfigDialog(
      config = config,
      onDismiss = { showEditDialog = false },
      onSave = { updated ->
        onConfigChange(updated)
        showEditDialog = false
      }
    )
  }
}

@Composable
private fun ConfigBadge(label: String, value: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF0B1320))
      .border(0.6.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Column {
      Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
      Text(
        text = value,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun EditConfigDialog(
  config: TunnelConfig,
  onDismiss: () -> Unit,
  onSave: (TunnelConfig) -> Unit
) {
  var name by remember { mutableStateOf(config.name) }
  var sshHost by remember { mutableStateOf(config.sshHost) }
  var sshPort by remember { mutableStateOf(config.sshPort.toString()) }
  var sshUser by remember { mutableStateOf(config.sshUsername) }
  var sshPass by remember { mutableStateOf(config.sshPassword) }
  var sniHost by remember { mutableStateOf(config.sniHost) }
  var payload by remember { mutableStateOf(config.payload) }
  var v2rayAddress by remember { mutableStateOf(config.v2rayAddress) }
  var v2rayPort by remember { mutableStateOf(config.v2rayPort.toString()) }
  var v2rayUuid by remember { mutableStateOf(config.v2rayUuid) }
  var v2rayPath by remember { mutableStateOf(config.v2rayWsPath) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Tunnel Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
    containerColor = ObsidianCard,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Profile Name") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldNeon,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        when (config.type) {
          TunnelType.SSH_DIRECT, TunnelType.SSH_SSL_TLS, TunnelType.SSH_HTTP_PROXY -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = sshHost,
                onValueChange = { sshHost = it },
                label = { Text("SSH Host") },
                modifier = Modifier.weight(1.5f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
              OutlinedTextField(
                value = sshPort,
                onValueChange = { sshPort = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = sshUser,
                onValueChange = { sshUser = it },
                label = { Text("Username") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
              OutlinedTextField(
                value = sshPass,
                onValueChange = { sshPass = it },
                label = { Text("Password") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
            }

            if (config.type == TunnelType.SSH_SSL_TLS) {
              OutlinedTextField(
                value = sniHost,
                onValueChange = { sniHost = it },
                label = { Text("SSL / SNI Bug Host (e.g. cdn.cloudflare.net)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CyanNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
            }

            if (config.type == TunnelType.SSH_HTTP_PROXY) {
              OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("HTTP Payload") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CyanNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
            }
          }

          TunnelType.V2RAY_VMESS, TunnelType.V2RAY_VLESS, TunnelType.TROJAN -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = v2rayAddress,
                onValueChange = { v2rayAddress = it },
                label = { Text("V2Ray Server") },
                modifier = Modifier.weight(1.5f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
              OutlinedTextField(
                value = v2rayPort,
                onValueChange = { v2rayPort = it },
                label = { Text("Port") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = EmeraldNeon,
                  unfocusedBorderColor = ObsidianBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                )
              )
            }

            OutlinedTextField(
              value = v2rayUuid,
              onValueChange = { v2rayUuid = it },
              label = { Text("UUID / Password") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldNeon,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )

            OutlinedTextField(
              value = v2rayPath,
              onValueChange = { v2rayPath = it },
              label = { Text("WebSocket Path") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val updated = config.copy(
            name = name,
            sshHost = sshHost,
            sshPort = sshPort.toIntOrNull() ?: 443,
            sshUsername = sshUser,
            sshPassword = sshPass,
            sniHost = sniHost,
            payload = payload,
            v2rayAddress = v2rayAddress,
            v2rayPort = v2rayPort.toIntOrNull() ?: 443,
            v2rayUuid = v2rayUuid,
            v2rayWsPath = v2rayPath
          )
          onSave(updated)
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
      ) {
        Text("Save Profile", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
