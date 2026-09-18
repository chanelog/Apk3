package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelConfig
import com.example.model.TunnelType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ConfigManager

@Composable
fun ConfigsScreen(
  activeConfig: TunnelConfig,
  onSelectConfig: (TunnelConfig) -> Unit,
  onImportNewConfig: (TunnelConfig) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var savedProfiles by remember { mutableStateOf(ConfigManager.getSavedProfiles(context)) }
  var showImportDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var configToExport by remember { mutableStateOf<TunnelConfig?>(null) }
  var editingConfig by remember { mutableStateOf<TunnelConfig?>(null) }

  // If user is editing a configuration, display the full Xray editor screen
  if (editingConfig != null) {
    XrayConfigEditorScreen(
      initialConfig = editingConfig!!,
      onSave = { updated ->
        val updatedList = savedProfiles.map { if (it.id == updated.id) updated else it }
        savedProfiles = updatedList
        ConfigManager.saveProfiles(context, updatedList)
        if (activeConfig.id == updated.id) {
          onSelectConfig(updated)
        }
        editingConfig = null
      },
      onDelete = { toDelete ->
        if (savedProfiles.size <= 1) {
          Toast.makeText(context, "Cannot delete last remaining profile.", Toast.LENGTH_SHORT).show()
        } else {
          val updatedList = savedProfiles.filter { it.id != toDelete.id }
          savedProfiles = updatedList
          ConfigManager.saveProfiles(context, updatedList)
          if (activeConfig.id == toDelete.id) {
            onSelectConfig(updatedList.first())
          }
          editingConfig = null
          Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
        }
      },
      onBack = {
        editingConfig = null
      }
    )
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Header Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Config Management",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Tap pencil to open full Xray editor",
          color = TextSecondary,
          fontSize = 11.sp
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
          onClick = {
            val newProfile = TunnelConfig(
              name = "New-Xray-Profile",
              type = TunnelType.V2RAY_VLESS,
              v2rayAddress = "id.dontol.ccwu.cc",
              v2rayPort = 443,
              v2rayUuid = "03567a4b-2056-4fd8-8826-0bca7d051760",
              v2rayWsPath = "/vless-ws",
              v2raySni = "listen.noice.id",
              v2rayTls = true,
              v2rayAllowInsecure = true,
              v2rayFingerprint = "chrome"
            )
            val updated = listOf(newProfile) + savedProfiles
            savedProfiles = updated
            ConfigManager.saveProfiles(context, updated)
            editingConfig = newProfile
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f), contentColor = CyanNeon),
          border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text("+ New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = { showImportDialog = true },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = {
            configToExport = activeConfig
            showExportDialog = true
          },
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard, contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Profiles List
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 36.dp)
    ) {
      items(savedProfiles, key = { it.id }) { profile ->
        val isActive = profile.id == activeConfig.id
        ProfileItemCard(
          profile = profile,
          isActive = isActive,
          onActivate = {
            onSelectConfig(profile)
            Toast.makeText(context, "Activated: ${profile.name}", Toast.LENGTH_SHORT).show()
          },
          onEdit = {
            editingConfig = profile
          },
          onExport = {
            configToExport = profile
            showExportDialog = true
          },
          onDelete = {
            if (savedProfiles.size <= 1) {
              Toast.makeText(context, "Cannot delete last remaining profile.", Toast.LENGTH_SHORT).show()
            } else {
              val updated = savedProfiles.filter { it.id != profile.id }
              savedProfiles = updated
              ConfigManager.saveProfiles(context, updated)
              if (isActive) {
                onSelectConfig(updated.first())
              }
              Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
            }
          }
        )
      }
    }
  }

  // Import Dialog
  if (showImportDialog) {
    ImportConfigDialog(
      onDismiss = { showImportDialog = false },
      onImport = { newConfig ->
        val updated = listOf(newConfig) + savedProfiles
        savedProfiles = updated
        ConfigManager.saveProfiles(context, updated)
        onImportNewConfig(newConfig)
        showImportDialog = false
        Toast.makeText(context, "Imported: ${newConfig.name}", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Export Dialog
  if (showExportDialog && configToExport != null) {
    ExportConfigDialog(
      config = configToExport!!,
      onDismiss = { showExportDialog = false }
    )
  }
}

@Composable
private fun ProfileItemCard(
  profile: TunnelConfig,
  isActive: Boolean,
  onActivate: () -> Unit,
  onEdit: () -> Unit,
  onExport: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF0D1B2A) else ObsidianCard),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isActive) 1.2.dp else 0.8.dp,
      color = if (isActive) EmeraldNeon else ObsidianBorder
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .clickable { onActivate() }
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = profile.name,
            color = if (isActive) EmeraldNeon else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          if (profile.isLocked) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(14.dp))
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = profile.type.displayName,
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
          if (profile.author.isNotBlank()) {
            Text(
              text = " • by ${profile.author}",
              color = TextSecondary,
              fontSize = 10.sp
            )
          }
        }

        if (profile.noteMessage.isNotBlank()) {
          Text(
            text = profile.noteMessage,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Default.Edit, contentDescription = "Edit Config", tint = EmeraldNeon, modifier = Modifier.size(17.dp))
        }

        IconButton(onClick = onExport, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Default.Share, contentDescription = "Export/Share", tint = CyanNeon, modifier = Modifier.size(17.dp))
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RubyNeon, modifier = Modifier.size(17.dp))
        }

        if (isActive) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Active",
            tint = EmeraldNeon,
            modifier = Modifier.size(20.dp).padding(start = 4.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ImportConfigDialog(
  onDismiss: () -> Unit,
  onImport: (TunnelConfig) -> Unit
) {
  val context = LocalContext.current
  var inputConfigText by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Import Configuration", color = TextPrimary, fontWeight = FontWeight.Bold) },
    containerColor = ObsidianCard,
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Paste .hnt config text, or vmess://, vless://, trojan:// URL link:",
          color = TextSecondary,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = inputConfigText,
          onValueChange = {
            inputConfigText = it
            errorMessage = null
          },
          placeholder = { Text("HNTUNNEL#... or vmess://...", color = TextMuted) },
          modifier = Modifier.fillMaxWidth(),
          minLines = 4,
          maxLines = 6,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldNeon,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = cm.primaryClip
              if (clip != null && clip.itemCount > 0) {
                inputConfigText = clip.getItemAt(0).text?.toString() ?: ""
              }
            }
          ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Paste from Clipboard", fontSize = 11.sp, color = CyanNeon)
          }
        }

        if (errorMessage != null) {
          Text(
            text = errorMessage!!,
            color = RubyNeon,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (inputConfigText.isBlank()) {
            errorMessage = "Please enter config text"
            return@Button
          }
          try {
            val parsed = ConfigManager.importConfig(inputConfigText)
            onImport(parsed)
          } catch (e: Exception) {
            errorMessage = "Failed to parse: ${e.localizedMessage}"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
      ) {
        Text("Import", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

@Composable
private fun ExportConfigDialog(
  config: TunnelConfig,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var configName by remember { mutableStateOf(config.name) }
  var author by remember { mutableStateOf(if (config.author.isBlank()) "HN User" else config.author) }
  var noteMessage by remember { mutableStateOf(config.noteMessage) }
  var lockSsh by remember { mutableStateOf(false) }
  var lockPayload by remember { mutableStateOf(false) }
  var expiryDate by remember { mutableStateOf("2026-12-31") }

  var exportedText by remember { mutableStateOf("") }
  var isExported by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Export Config (.hnt)", color = TextPrimary, fontWeight = FontWeight.Bold) },
    containerColor = ObsidianCard,
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (!isExported) {
          OutlinedTextField(
            value = configName,
            onValueChange = { configName = it },
            label = { Text("Config Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Author / Creator") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = noteMessage,
            onValueChange = { noteMessage = it },
            label = { Text("Note / Message for Users") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = EmeraldNeon,
              unfocusedBorderColor = ObsidianBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Lock Options
          Text("SECURITY & LOCK OPTIONS", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { lockSsh = !lockSsh }
          ) {
            Checkbox(
              checked = lockSsh,
              onCheckedChange = { lockSsh = it },
              colors = CheckboxDefaults.colors(checkedColor = EmeraldNeon)
            )
            Text("Lock SSH / V2Ray Credentials", color = TextPrimary, fontSize = 12.sp)
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { lockPayload = !lockPayload }
          ) {
            Checkbox(
              checked = lockPayload,
              onCheckedChange = { lockPayload = it },
              colors = CheckboxDefaults.colors(checkedColor = EmeraldNeon)
            )
            Text("Lock Payload Bug (Hide from user)", color = TextPrimary, fontSize = 12.sp)
          }
        } else {
          Text("Config exported successfully! You can share or copy below:", color = TextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF070B12))
              .border(0.8.dp, CyanNeon, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Text(
              text = exportedText.take(160) + "...",
              color = CyanNeon,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    },
    confirmButton = {
      if (!isExported) {
        Button(
          onClick = {
            val toExport = config.copy(
              name = configName,
              author = author,
              noteMessage = noteMessage,
              isLocked = lockSsh || lockPayload,
              lockSsh = lockSsh,
              lockPayload = lockPayload,
              expiryDate = expiryDate
            )
            exportedText = ConfigManager.exportToHnt(toExport)
            isExported = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
        ) {
          Text("Generate .hnt", fontWeight = FontWeight.Bold)
        }
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              cm.setPrimaryClip(ClipData.newPlainText("HN Tunnel Config", exportedText))
              Toast.makeText(context, "Config copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianDark)
          ) {
            Text("Copy", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "HN Tunnel Config - $configName")
                putExtra(Intent.EXTRA_TEXT, exportedText)
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share HN Tunnel Config"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
          ) {
            Text("Share", fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondary)
      }
    }
  )
}
