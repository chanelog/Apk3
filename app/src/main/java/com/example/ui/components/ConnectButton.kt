package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TunnelState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.RubyNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConnectButton(
  state: TunnelState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (state == TunnelState.CONNECTING || state == TunnelState.AUTHENTICATING || state == TunnelState.HANDSHAKING) 1.14f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = if (state == TunnelState.CONNECTED) 0.6f else 0.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  val ringColor by animateColorAsState(
    targetValue = when (state) {
      TunnelState.CONNECTED -> EmeraldNeon
      TunnelState.CONNECTING, TunnelState.AUTHENTICATING, TunnelState.HANDSHAKING -> CyanNeon
      TunnelState.STOPPING -> RubyNeon
      TunnelState.DISCONNECTED -> ObsidianBorder
    },
    label = "ring_color"
  )

  val buttonBackground by animateColorAsState(
    targetValue = when (state) {
      TunnelState.CONNECTED -> Color(0xFF06382C)
      TunnelState.CONNECTING, TunnelState.AUTHENTICATING, TunnelState.HANDSHAKING -> Color(0xFF0E2E3B)
      TunnelState.STOPPING -> Color(0xFF331518)
      TunnelState.DISCONNECTED -> ObsidianCard
    },
    label = "btn_bg"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(160.dp)
    ) {
      // Ambient outer pulse halo
      Box(
        modifier = Modifier
          .size(150.dp)
          .scale(pulseScale)
          .clip(CircleShape)
          .background(ringColor.copy(alpha = pulseAlpha))
      )

      // Main tactile button
      Box(
        modifier = Modifier
          .size(126.dp)
          .clip(CircleShape)
          .background(buttonBackground)
          .border(
            width = 3.dp,
            brush = Brush.sweepGradient(
              colors = listOf(
                ringColor,
                ringColor.copy(alpha = 0.4f),
                ringColor
              )
            ),
            shape = CircleShape
          )
          .testTag("connect_tunnel_button")
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = ringColor)
          ) {
            onClick()
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (state == TunnelState.CONNECTED) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
          contentDescription = "Connect or Disconnect",
          tint = if (state == TunnelState.DISCONNECTED) TextSecondary else ringColor,
          modifier = Modifier.size(54.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = state.label,
      color = when (state) {
        TunnelState.CONNECTED -> EmeraldNeon
        TunnelState.CONNECTING, TunnelState.AUTHENTICATING, TunnelState.HANDSHAKING -> CyanNeon
        TunnelState.STOPPING -> RubyNeon
        TunnelState.DISCONNECTED -> TextSecondary
      },
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )

    Text(
      text = if (state == TunnelState.CONNECTED) "TAP TO STOP" else "TAP TO CONNECT",
      color = TextSecondary.copy(alpha = 0.7f),
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.2.sp,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}
