package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ClinicDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF003822),
    primaryContainer = NeonGreenDim,
    onPrimaryContainer = Color(0xFF70FFBE),
    secondary = NeonViolet,
    onSecondary = Color(0xFF3B0764),
    secondaryContainer = NeonVioletDim,
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = CyberTeal,
    onTertiary = Color(0xFF003731),
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass,
    outlineVariant = BorderGlassWhite,
    error = StatusError,
    onError = Color.White
)

@Composable
fun AlQaafiClinicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClinicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
