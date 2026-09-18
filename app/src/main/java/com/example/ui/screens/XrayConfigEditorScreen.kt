package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelConfig
import com.example.model.TunnelType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun XrayConfigEditorScreen(
  initialConfig: TunnelConfig,
  onSave: (TunnelConfig) -> Unit,
  onDelete: (TunnelConfig) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var remarks by remember { mutableStateOf(initialConfig.name) }
  var address by remember { mutableStateOf(initialConfig.v2rayAddress) }
  var port by remember { mutableStateOf(initialConfig.v2rayPort.toString()) }
  var id by remember { mutableStateOf(initialConfig.v2rayUuid) }
  var flow by remember { mutableStateOf(if (initialConfig.v2rayFlow.isNotBlank()) initialConfig.v2rayFlow else "none") }
  var encryption by remember { mutableStateOf(if (initialConfig.v2rayEncryption.isNotBlank()) initialConfig.v2rayEncryption else "none") }

  // Transport section
  var network by remember { mutableStateOf(if (initialConfig.v2rayNetwork.isNotBlank()) initialConfig.v2rayNetwork else "ws") }
  var headerType by remember { mutableStateOf(if (initialConfig.v2rayHeaderType.isNotBlank()) initialConfig.v2rayHeaderType else "---") }
  var requestHost by remember { mutableStateOf(if (initialConfig.v2rayRequestHost.isNotBlank()) initialConfig.v2rayRequestHost else initialConfig.v2rayAddress) }
  var path by remember { mutableStateOf(if (initialConfig.v2rayWsPath.isNotBlank()) initialConfig.v2rayWsPath else "/v2ray-ws") }
  var tls by remember { mutableStateOf(if (initialConfig.v2rayTls) "tls" else "none") }
  var sni by remember { mutableStateOf(if (initialConfig.v2raySni.isNotBlank()) initialConfig.v2raySni else initialConfig.sniHost) }
  var allowInsecure by remember { mutableStateOf(initialConfig.v2rayAllowInsecure.toString()) }
  var fingerprint by remember { mutableStateOf(if (initialConfig.v2rayFingerprint.isNotBlank()) initialConfig.v2rayFingerprint else "chrome") }

  var showDeleteDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .statusBarsPadding()
  ) {
    // 1. Top Bar matching screenshot
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Configuration file",
          color = TextPrimary,
          fontSize = 19.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { showDeleteDialog = true }) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = TextPrimary
          )
        }

        IconButton(
          onClick = {
            if (remarks.isBlank()) {
              Toast.makeText(context, "Please enter remarks name", Toast.LENGTH_SHORT).show()
              return@IconButton
            }
            val parsedPort = port.toIntOrNull() ?: 443
            val updated = initialConfig.copy(
              name = remarks.trim(),
              v2rayAddress = address.trim(),
              v2rayPort = parsedPort,
              v2rayUuid = id.trim(),
              v2rayFlow = flow,
              v2rayEncryption = encryption,
              v2rayNetwork = network,
              v2rayHeaderType = headerType,
              v2rayRequestHost = requestHost.trim(),
              v2rayWsPath = path.trim(),
              v2rayTls = (tls != "none"),
              v2raySni = sni.trim(),
              v2rayAllowInsecure = (allowInsecure == "true"),
              v2rayFingerprint = fingerprint
            )
            onSave(updated)
            Toast.makeText(context, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
          }
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Save",
            tint = EmeraldNeon
          )
        }
      }
    }

    // 2. Form Content matching exact screenshot
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // remarks
      EditorTextField(
        label = "remarks",
        value = remarks,
        onValueChange = { remarks = it }
      )

      // address
      EditorTextField(
        label = "address",
        value = address,
        onValueChange = { address = it }
      )

      // port
      EditorTextField(
        label = "port",
        value = port,
        onValueChange = { port = it },
        keyboardType = KeyboardType.Number
      )

      // id
      EditorTextField(
        label = "id",
        value = id,
        onValueChange = { id = it }
      )

      // flow dropdown
      EditorDropdownField(
        label = "flow",
        selected = flow,
        options = listOf("none", "xtls-rprx-vision", "xtls-rprx-vision-udp443"),
        onSelect = { flow = it }
      )

      // encryption dropdown
      EditorDropdownField(
        label = "encryption",
        selected = encryption,
        options = listOf("none", "auto", "zero", "aes-128-gcm", "chacha20-poly1305"),
        onSelect = { encryption = it }
      )

      // Subheading: Transport
      Text(
        text = "Transport",
        color = EmeraldNeon,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
      )

      // Network dropdown
      EditorDropdownField(
        label = "Network",
        selected = network,
        options = listOf("ws", "tcp", "grpc", "http", "quic", "kcp", "httpupgrade"),
        onSelect = { network = it }
      )

      // head type dropdown
      EditorDropdownField(
        label = "head type",
        selected = headerType,
        options = listOf("---", "http", "srtp", "utp", "wechat-video", "dtls", "wireguard"),
        onSelect = { headerType = it }
      )

      // request host(host/ws host/h2 host)/QUIC security
      EditorTextField(
        label = "request host(host/ws host/h2 host)/QUIC security",
        value = requestHost,
        onValueChange = { requestHost = it }
      )

      // path(ws path/h2 path)/QUIC key/kcp seed/gRPC serviceName
      EditorTextField(
        label = "path(ws path/h2 path)/QUIC key/kcp seed/gRPC serviceName",
        value = path,
        onValueChange = { path = it }
      )

      // tls dropdown
      EditorDropdownField(
        label = "tls",
        selected = tls,
        options = listOf("none", "tls", "reality"),
        onSelect = { tls = it }
      )

      // SNI
      EditorTextField(
        label = "SNI",
        value = sni,
        onValueChange = { sni = it }
      )

      // allowInsecure dropdown
      EditorDropdownField(
        label = "allowInsecure",
        selected = allowInsecure,
        options = listOf("true", "false"),
        onSelect = { allowInsecure = it }
      )

      // Fingerprint dropdown
      EditorDropdownField(
        label = "Fingerprint",
        selected = fingerprint,
        options = listOf("chrome", "firefox", "safari", "ios", "android", "edge", "randomized"),
        onSelect = { fingerprint = it }
      )
    }
  }

  // Delete Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Configuration", color = TextPrimary) },
      text = { Text("Are you sure you want to delete profile '${initialConfig.name}'?", color = TextSecondary) },
      containerColor = ObsidianCard,
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDelete(initialConfig)
          }
        ) {
          Text("Delete", color = RubyNeon, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun EditorTextField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  keyboardType: KeyboardType = KeyboardType.Text
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = {
      Text(
        text = label,
        color = TextSecondary,
        fontSize = 12.sp,
        maxLines = 1
      )
    },
    modifier = Modifier.fillMaxWidth(),
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Composable
private fun EditorDropdownField(
  label: String,
  selected: String,
  options: List<String>,
  onSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = selected,
      onValueChange = {},
      readOnly = true,
      label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
      trailingIcon = {
        IconButton(onClick = { expanded = true }) {
          Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = true },
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

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier
        .background(Color(0xFF0F172A))
        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = {
            Text(
              text = option,
              color = if (option == selected) EmeraldNeon else TextPrimary,
              fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
            )
          },
          onClick = {
            onSelect(option)
            expanded = false
          }
        )
      }
    }
  }
}
