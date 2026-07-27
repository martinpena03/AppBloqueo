package com.freno.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta anti-dopamina: escala de grises, sin acentos de color.
 * Cualquier elemento "primario" es simplemente un gris oscuro/claro, nada estimulante.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E2E2E),
    onPrimary = Color(0xFFF4F4F4),
    secondary = Color(0xFF5A5A5A),
    onSecondary = Color(0xFFF4F4F4),
    background = Color(0xFFF4F4F4),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE7E7E7),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFFCFCFCF),
    error = Color(0xFF6B6B6B),
    onError = Color(0xFFF4F4F4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9C9C9),
    onPrimary = Color(0xFF161616),
    secondary = Color(0xFF9A9A9A),
    onSecondary = Color(0xFF161616),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAFAFAF),
    outline = Color(0xFF3A3A3A),
    error = Color(0xFFB0B0B0),
    onError = Color(0xFF161616)
)

private val FrenoTypography = Typography()

@Composable
fun FrenoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FrenoTypography,
        content = content
    )
}
