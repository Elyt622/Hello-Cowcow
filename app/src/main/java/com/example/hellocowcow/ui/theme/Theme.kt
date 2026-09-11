package com.example.hellocowcow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = CowDarkPrimary,
  onPrimary = CowDarkOnPrimary,
  primaryContainer = CowDarkPrimaryContainer,
  onPrimaryContainer = CowDarkOnPrimaryContainer,
  secondary = CowDarkSecondary,
  onSecondary = Color(0xFF3E3300),
  secondaryContainer = CowDarkSecondaryContainer,
  onSecondaryContainer = CowDarkOnSecondaryContainer,
  background = CowDarkBackground,
  onBackground = CowDarkOnSurface,
  surface = CowDarkSurface,
  onSurface = CowDarkOnSurface,
  surfaceVariant = CowDarkSurfaceVariant,
  onSurfaceVariant = CowDarkOnSurfaceVariant,
  outline = CowDarkOutline
)

private val LightColorScheme = lightColorScheme(
  primary = CustomRed,
  onPrimary = Color.White,
  primaryContainer = CowLightPrimaryContainer,
  onPrimaryContainer = CowLightOnPrimaryContainer,
  secondary = CowLightSecondary,
  onSecondary = Color.White,
  secondaryContainer = CowLightSecondaryContainer,
  onSecondaryContainer = CowLightOnSecondaryContainer,
  background = CowLightBackground,
  onBackground = CowLightOnSurface,
  surface = CowLightSurface,
  onSurface = CowLightOnSurface,
  surfaceVariant = CowLightSurfaceVariant,
  onSurfaceVariant = CowLightOnSurfaceVariant,
  outline = CowLightOutline
)

@Composable
fun HelloCowCowTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography2,
    content = content
  )
}
