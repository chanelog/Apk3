package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.TunnelState
import com.example.tunnel.TunnelController
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
import com.example.util.LogManager

@Composable
fun LogsScreen(
  logs: List<LogEntry>,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
  val currentState = TunnelController.tunnelState.value
  val currentStats = TunnelController.tunnelStats.value

  val safeLogs = remember(logs) { ArrayList(logs) }
  val filteredLogs = safeLogs.filter { entry ->
    selectedFilter == null || entry.level == selectedFilter
  }

  LaunchedEffect(filteredLogs.size) {
    if (filteredLogs.isNotEmpty()) {
      try {
        listState.scrollToItem(filteredLogs.size - 1)
      } catch (_: Throwable) {
        // Prevent scroll crash
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    // Header & Connection Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Console Log", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
          Spacer(modifier = Modifier.width(6.dp))
          Text("(${filteredLogs.size})", color = TextSecondary, fontSize = 12.sp)
        }

        // Live status pill
        val (statusText, statusColor) = when (currentState) {
          TunnelState.CONNECTED -> "CONNECTED • ${currentStats.pingMs}ms" to EmeraldNeon
          TunnelState.CONNECTING -> "CONNECTING..." to CyanNeon
          TunnelState.AUTHENTICATING -> "AUTHENTICATING..." to CyanNeon
          TunnelState.HANDSHAKING -> "HANDSHAKING..." to CyanNeon
          TunnelState.STOPPING -> "DISCONNECTING..." to RubyNeon
          TunnelState.DISCONNECTED -> "DISCONNECTED" to TextMuted
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 3.dp)
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = statusText,
            color = statusColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
          onClick = {
            try {
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              cm.setPrimaryClip(ClipData.newPlainText("HN Tunnel Logs", LogManager.getAllLogsAsText()))
              Toast.makeText(context, "All logs copied to clipboard", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
              Toast.makeText(context, "Could not copy logs", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanNeon, modifier = Modifier.size(18.dp))
        }

        IconButton(
          onClick = onClearLogs,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.ClearAll, contentDescription = "Clear", tint = RubyNeon, modifier = Modifier.size(20.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      FilterChipItem(label = "ALL", isSelected = selectedFilter == null) { selectedFilter = null }
      FilterChipItem(label = "INFO", isSelected = selectedFilter == LogLevel.INFO) { selectedFilter = LogLevel.INFO }
      FilterChipItem(label = "SUCCESS", isSelected = selectedFilter == LogLevel.SUCCESS) { selectedFilter = LogLevel.SUCCESS }
      FilterChipItem(label = "WARN", isSelected = selectedFilter == LogLevel.WARN) { selectedFilter = LogLevel.WARN }
      FilterChipItem(label = "ERROR", isSelected = selectedFilter == LogLevel.ERROR) { selectedFilter = LogLevel.ERROR }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Terminal Container
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF060911))
        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
        .padding(8.dp)
    ) {
      if (filteredLogs.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "Console log is ready.",
              color = TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap connect on Tunnel tab to view live connection handshakes.",
              color = TextMuted,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(
            items = filteredLogs,
            key = { log -> "${log.id}_${log.timestamp}" }
          ) { log ->
            LogLineItem(
              log = log,
              onItemClick = {
                try {
                  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  cm.setPrimaryClip(ClipData.newPlainText("HN Log", "[${log.timestamp}] ${log.message}"))
                  Toast.makeText(context, "Log line copied", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LogLineItem(log: LogEntry, onItemClick: () -> Unit) {
  val levelColor = when (log.level) {
    LogLevel.SUCCESS -> EmeraldNeon
    LogLevel.INFO -> CyanNeon
    LogLevel.DEBUG -> TextSecondary
    LogLevel.WARN -> AmberNeon
    LogLevel.ERROR -> RubyNeon
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .clickable { onItemClick() }
      .padding(vertical = 2.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = "[${log.timestamp}]",
      color = TextMuted,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Normal
    )

    Spacer(modifier = Modifier.width(6.dp))

    Text(
      text = log.message,
      color = levelColor,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      lineHeight = 16.sp
    )
  }
}

@Composable
private fun FilterChipItem(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else ObsidianCard)
      .border(0.8.dp, if (isSelected) CyanNeon else ObsidianBorder, RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      color = if (isSelected) CyanNeon else TextSecondary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
