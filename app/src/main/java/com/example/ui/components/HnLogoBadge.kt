package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard

@Composable
fun HnLogoBadge(
  size: Dp = 40.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.28f))
      .background(
        Brush.linearGradient(
          colors = listOf(
            ObsidianCard,
            Color(0xFF042F2E)
          )
        )
      )
      .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            EmeraldNeon,
            CyanNeon,
            ObsidianBorder
          )
        ),
        shape = RoundedCornerShape(size * 0.28f)
      ),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "HN",
      color = EmeraldNeon,
      fontSize = (size.value * 0.44f).sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      letterSpacing = (-1.5).sp
    )
  }
}
