package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelStats
import com.example.ui.theme.AmberNeon
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TrafficMeter(
  stats: TunnelStats,
  isConnected: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(ObsidianCard)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Column {
      // Top row: Download & Upload speeds
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        StatCard(
          title = "DOWNLOAD",
          value = if (isConnected) stats.formatSpeed(stats.speedInBps) else "0.0 KB/s",
          total = "Total: ${stats.formatBytes(stats.bytesIn)}",
          icon = Icons.Default.ArrowDownward,
          accentColor = CyanNeon,
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        StatCard(
          title = "UPLOAD",
          value = if (isConnected) stats.formatSpeed(stats.speedOutBps) else "0.0 KB/s",
          total = "Total: ${stats.formatBytes(stats.bytesOut)}",
          icon = Icons.Default.ArrowUpward,
          accentColor = EmeraldNeon,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom row: Duration and Ping
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Duration Pill
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(0.8.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stats.formatDuration(),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )
        }

        // Ping Pill
        val pingColor = when {
          stats.pingMs in 1..80 -> EmeraldNeon
          stats.pingMs in 81..160 -> AmberNeon
          stats.pingMs > 160 -> RubyNeon
          else -> TextSecondary
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A))
            .border(0.8.dp, ObsidianBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(pingColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isConnected && stats.pingMs > 0) "${stats.pingMs} ms" else "--- ms",
            color = pingColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
private fun StatCard(
  title: String,
  value: String,
  total: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF0F172A))
      .border(0.8.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          color = TextSecondary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace
      )
      Text(
        text = total,
        color = TextSecondary.copy(alpha = 0.8f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
