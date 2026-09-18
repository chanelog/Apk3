package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldNeon,
  onPrimary = ObsidianDark,
  primaryContainer = EmeraldDark,
  onPrimaryContainer = TextPrimary,
  secondary = CyanNeon,
  onSecondary = ObsidianDark,
  secondaryContainer = ObsidianCard,
  onSecondaryContainer = CyanNeon,
  tertiary = VioletNeon,
  onTertiary = ObsidianDark,
  background = ObsidianDark,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianCard,
  onSurfaceVariant = TextSecondary,
  outline = ObsidianBorder,
  error = RubyNeon,
  onError = ObsidianDark,
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldDark,
  onPrimary = TextPrimary,
  primaryContainer = EmeraldNeon,
  onPrimaryContainer = ObsidianDark,
  secondary = CyanNeon,
  onSecondary = ObsidianDark,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightCard,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorder,
  error = RubyNeon,
  onError = TextPrimary,
)

@Composable
fun HnTunnelTheme(
  darkTheme: Boolean = true, // Default to dark cyber theme for tunnel application
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
