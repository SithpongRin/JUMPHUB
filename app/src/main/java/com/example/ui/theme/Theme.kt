package com.example.ui.theme

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
    primary = ElectricLime,
    onPrimary = DarkBackground,
    primaryContainer = ElectricLimeDark,
    onPrimaryContainer = ElectricLimeBright,
    secondary = CyberCyan,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = SportAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkSurfaceBorder,
    error = SportRed
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricLimeDark,
    onPrimary = LightSurface,
    primaryContainer = ElectricLimeBright,
    onPrimaryContainer = Color(0xFF14532D),
    secondary = CyberCyanDark,
    onSecondary = LightSurface,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFFD97706),
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightSurfaceBorder,
    error = SportRed
)

@Composable
fun JUMPHUBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep athletic branded identity
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
        typography = Typography,
        content = content
    )
}
