package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelConfig
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
  config: TunnelConfig,
  onDismiss: () -> Unit,
  onSave: (TunnelConfig) -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var autoPingEnabled by remember { mutableStateOf(config.autoPingEnabled) }
  var autoPingHost by remember { mutableStateOf(if (config.autoPingHost.isNotBlank()) config.autoPingHost else "8.8.8.8") }
  var autoPingInterval by remember { mutableStateOf(config.autoPingIntervalSec) }

  var enableUdp by remember { mutableStateOf(config.enableUdp) }
  var udpgwPort by remember { mutableStateOf(config.udpgwPort.toString()) }
  var tlsVersion by remember { mutableStateOf(config.tlsVersion) }
  var sendBuffer by remember { mutableStateOf(config.sendBuffer.toString()) }
  var receiveBuffer by remember { mutableStateOf(config.receiveBuffer.toString()) }
  var autoReconnect by remember { mutableStateOf(config.autoReconnect) }
  var routingMode by remember { mutableStateOf(config.routingMode) }
  var customDns1 by remember { mutableStateOf(config.customDns1) }
  var customDns2 by remember { mutableStateOf(config.customDns2) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("App Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    containerColor = ObsidianCard,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // --- Section 1: Auto Ping ---
        Text(
          text = "AUTO PING & KEEP-ALIVE",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.NetworkPing, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Enable Auto Ping", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Periodic latency check to keep tunnel alive", color = TextSecondary, fontSize = 10.sp)
            }
          }

          Switch(
            checked = autoPingEnabled,
            onCheckedChange = { autoPingEnabled = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = EmeraldNeon,
              uncheckedTrackColor = ObsidianSurface
            )
          )
        }

        if (autoPingEnabled) {
          OutlinedTextField(
            value = autoPingHost,
            onValueChange = { autoPingHost = it },
            label = { Text("Auto Ping Host Target") },
            placeholder = { Text("e.g. 8.8.8.8 or 1.1.1.1") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )

          Column {
            Text("Auto Ping Interval:", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(2 to "2 sec", 3 to "3 sec", 5 to "5 sec", 10 to "10 sec").forEach { (sec, label) ->
                val selected = autoPingInterval == sec
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) EmeraldNeon.copy(alpha = 0.2f) else ObsidianDark)
                    .border(if (selected) 1.2.dp else 0.8.dp, if (selected) EmeraldNeon else ObsidianBorder, RoundedCornerShape(8.dp))
                    .clickable { autoPingInterval = sec }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = label,
                    color = if (selected) EmeraldNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(ObsidianBorder))

        // --- Section 2: Tunnel Behavior ---
        Text(
          text = "TUNNEL BEHAVIOR",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Router, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("UDP Forwarding", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Required for games & voice calls", color = TextSecondary, fontSize = 10.sp)
            }
          }

          Switch(
            checked = enableUdp,
            onCheckedChange = { enableUdp = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = CyanNeon,
              uncheckedTrackColor = ObsidianSurface
            )
          )
        }

        if (enableUdp) {
          OutlinedTextField(
            value = udpgwPort,
            onValueChange = { udpgwPort = it.filter { ch -> ch.isDigit() } },
            label = { Text("UDPGW Port") },
            placeholder = { Text("7300") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(ObsidianBorder))

        // --- Section 2.1: Buffer & TLS Tuning ---
        Text(
          text = "TLS & BUFFER TUNING",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        // TLS Version selector
        Column {
          Text("TLS Version", color = TextSecondary, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Auto", "TLSv1.2", "TLSv1.3").forEach { ver ->
              val selected = tlsVersion == ver
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (selected) CyanNeon.copy(alpha = 0.2f) else ObsidianDark)
                  .border(if (selected) 1.2.dp else 0.8.dp, if (selected) CyanNeon else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { tlsVersion = ver }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  text = ver,
                  color = if (selected) CyanNeon else TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }
        }

        // Send & Receive Buffer
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = sendBuffer,
            onValueChange = { sendBuffer = it.filter { ch -> ch.isDigit() } },
            label = { Text("Send buffer") },
            placeholder = { Text("16384") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )

          OutlinedTextField(
            value = receiveBuffer,
            onValueChange = { receiveBuffer = it.filter { ch -> ch.isDigit() } },
            label = { Text("Receive buffer") },
            placeholder = { Text("32768") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Auto Reconnect", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("Reconnect when connection drops", color = TextSecondary, fontSize = 10.sp)
            }
          }

          Switch(
            checked = autoReconnect,
            onCheckedChange = { autoReconnect = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = EmeraldNeon,
              uncheckedTrackColor = ObsidianSurface
            )
          )
        }

        // Routing Mode Selector
        Column {
          Text("INTERNET ROUTING STRATEGY", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (routingMode == "DIRECT_TUNNEL") "Direct Mode: Internet browsing, YouTube & apps stay active with live keep-alive tunnel" else "Global VPN: Capture and route 100% OS traffic into virtual TUN interface",
            color = TextSecondary,
            fontSize = 10.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("DIRECT_TUNNEL" to "Direct / Web Active", "GLOBAL_TUN" to "Global Full TUN").forEach { (mode, label) ->
              val selected = routingMode == mode
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (selected) EmeraldNeon.copy(alpha = 0.2f) else ObsidianDark)
                  .border(if (selected) 1.2.dp else 0.8.dp, if (selected) EmeraldNeon else ObsidianBorder, RoundedCornerShape(8.dp))
                  .clickable { routingMode = mode }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = label,
                  color = if (selected) EmeraldNeon else TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(ObsidianBorder))

        // --- Section 3: DNS ---
        Text(
          text = "CUSTOM DNS SERVERS",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = customDns1,
            onValueChange = { customDns1 = it },
            label = { Text("DNS 1") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )

          OutlinedTextField(
            value = customDns2,
            onValueChange = { customDns2 = it },
            label = { Text("DNS 2") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A)
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val updated = config.copy(
            autoPingEnabled = autoPingEnabled,
            autoPingHost = autoPingHost.trim(),
            autoPingIntervalSec = autoPingInterval,
            enableUdp = enableUdp,
            udpgwPort = udpgwPort.toIntOrNull() ?: 7300,
            tlsVersion = tlsVersion,
            sendBuffer = sendBuffer.toIntOrNull() ?: 16384,
            receiveBuffer = receiveBuffer.toIntOrNull() ?: 32768,
            autoReconnect = autoReconnect,
            routingMode = routingMode,
            customDns1 = customDns1.trim(),
            customDns2 = customDns2.trim()
          )
          onSave(updated)
          Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
