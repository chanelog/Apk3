package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelConfig
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BugScanResult
import com.example.util.NetworkInfo
import com.example.util.NetworkTools
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
  config: TunnelConfig,
  onUpdateDns: (provider: String, dns1: String, dns2: String) -> Unit,
  onUpdateAutoPing: ((enabled: Boolean, host: String, intervalSec: Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  // Scanner state
  var testHost by remember { mutableStateOf(if (config.sniHost.isNotBlank()) config.sniHost else "cdn.cloudflare.net") }
  var testPort by remember { mutableStateOf("443") }
  var isScanning by remember { mutableStateOf(false) }
  var scanResult by remember { mutableStateOf<BugScanResult?>(null) }

  // Auto Ping state
  var autoPingEnabled by remember(config.autoPingEnabled) { mutableStateOf(config.autoPingEnabled) }
  var autoPingHost by remember(config.autoPingHost) { mutableStateOf(if (config.autoPingHost.isNotBlank()) config.autoPingHost else "8.8.8.8") }
  var autoPingInterval by remember(config.autoPingIntervalSec) { mutableStateOf(config.autoPingIntervalSec) }

  // Network Info state
  var networkInfo by remember { mutableStateOf(NetworkTools.getNetworkInfo(context)) }

  // DNS Presets
  var selectedDns by remember { mutableStateOf(config.dnsProvider) }
  var customDns1 by remember { mutableStateOf(config.customDns1) }
  var customDns2 by remember { mutableStateOf(config.customDns2) }

  LaunchedEffect(Unit) {
    networkInfo = NetworkTools.getNetworkInfo(context)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .padding(bottom = 36.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Radar, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Network & Bug Tools", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    Text("Scan SNI/Bug hosts, auto ping keep-alive, and custom DNS", color = TextSecondary, fontSize = 11.sp)

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Auto Ping & Keep-Alive Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, if (autoPingEnabled) EmeraldNeon.copy(alpha = 0.5f) else ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NetworkPing, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("AUTO PING (KEEP-ALIVE)", color = EmeraldNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
          }

          androidx.compose.material3.Switch(
            checked = autoPingEnabled,
            onCheckedChange = {
              autoPingEnabled = it
              onUpdateAutoPing?.invoke(it, autoPingHost, autoPingInterval)
            },
            colors = androidx.compose.material3.SwitchDefaults.colors(
              checkedThumbColor = ObsidianDark,
              checkedTrackColor = EmeraldNeon
            )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Pings the host in background to maintain active packets and prevent tunnel timeout.",
          color = TextSecondary,
          fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = autoPingHost,
          onValueChange = {
            autoPingHost = it
            onUpdateAutoPing?.invoke(autoPingEnabled, it, autoPingInterval)
          },
          label = { Text("Auto Ping Host Target") },
          singleLine = true,
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Ping Interval:", color = TextSecondary, fontSize = 11.sp)

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
              val selected = autoPingInterval == sec
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (selected) EmeraldNeon.copy(alpha = 0.2f) else ObsidianDark)
                  .border(if (selected) 1.dp else 0.6.dp, if (selected) EmeraldNeon else ObsidianBorder, RoundedCornerShape(6.dp))
                  .clickable {
                    autoPingInterval = sec
                    onUpdateAutoPing?.invoke(autoPingEnabled, autoPingHost, sec)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
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
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 1. Bug Host / SNI Scanner
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.NetworkPing, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("HOST / SNI CHECKER", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = testHost,
            onValueChange = { testHost = it },
            label = { Text("Host or Bug") },
            placeholder = { Text("e.g. cdn.cloudflare.net") },
            modifier = Modifier.weight(2f),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          OutlinedTextField(
            value = testPort,
            onValueChange = { testPort = it },
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

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = {
            isScanning = true
            coroutineScope.launch {
              val port = testPort.toIntOrNull() ?: 443
              scanResult = NetworkTools.scanHost(testHost, port, "HEAD")
              isScanning = false
            }
          },
          enabled = !isScanning && testHost.isNotBlank(),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
        ) {
          if (isScanning) {
            CircularProgressIndicator(color = ObsidianDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Testing Host...", fontWeight = FontWeight.Bold)
          } else {
            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Scan / Check Bug Host", fontWeight = FontWeight.Bold)
          }
        }

        // Result display
        scanResult?.let { result ->
          Spacer(modifier = Modifier.height(12.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF070B12))
              .border(
                1.dp,
                if (result.responseCode in 200..399) EmeraldNeon else RubyNeon,
                RoundedCornerShape(10.dp)
              )
              .padding(12.dp)
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (result.responseCode > 0) "HTTP ${result.responseCode} ${result.responseMessage}" else "Connection Failed",
                  color = if (result.responseCode in 200..399) EmeraldNeon else RubyNeon,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )

                Text(
                  text = "${result.latencyMs} ms",
                  color = CyanNeon,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Server: ${result.serverHeader}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )

              if (result.errorMsg != null) {
                Text(
                  text = "Error: ${result.errorMsg}",
                  color = RubyNeon,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. DNS Customizer
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Dns, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("DNS FORWARDING & RESOLVER", color = EmeraldNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        val dnsPresets = listOf(
          Triple("Google DNS", "8.8.8.8", "8.8.4.4"),
          Triple("Cloudflare", "1.1.1.1", "1.0.0.1"),
          Triple("AdGuard (AdBlock)", "94.140.14.14", "94.140.15.15"),
          Triple("Quad9", "9.9.9.9", "149.112.112.112"),
          Triple("Custom", customDns1, customDns2)
        )

        dnsPresets.forEach { (name, d1, d2) ->
          val isSel = selectedDns == name
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSel) EmeraldNeon.copy(alpha = 0.15f) else Color.Transparent)
              .clickable {
                selectedDns = name
                if (name != "Custom") {
                  customDns1 = d1
                  customDns2 = d2
                  onUpdateDns(name, d1, d2)
                }
              }
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(name, color = if (isSel) EmeraldNeon else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text("$d1, $d2", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            if (isSel) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
            }
          }
        }

        if (selectedDns == "Custom") {
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customDns1,
              onValueChange = {
                customDns1 = it
                onUpdateDns("Custom", it, customDns2)
              },
              label = { Text("Primary DNS") },
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, unfocusedBorderColor = ObsidianBorder)
            )
            OutlinedTextField(
              value = customDns2,
              onValueChange = {
                customDns2 = it
                onUpdateDns("Custom", customDns1, it)
              },
              label = { Text("Secondary DNS") },
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, unfocusedBorderColor = ObsidianBorder)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Network Interface Info
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Router, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("DEVICE & NETWORK INTERFACE", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Interface Type", color = TextSecondary, fontSize = 12.sp)
          Text(networkInfo.connectionType, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Local IPv4", color = TextSecondary, fontSize = 12.sp)
          Text(networkInfo.localIp, color = EmeraldNeon, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Active System DNS", color = TextSecondary, fontSize = 12.sp)
          Text(networkInfo.dnsServers.joinToString(", "), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
      }
    }
  }
}
