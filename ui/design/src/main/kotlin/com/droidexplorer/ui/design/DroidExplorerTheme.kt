package com.droidexplorer.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Original Droid Explorer design system, following Windows 11 patterns:
 * neutral surfaces, a single accent colour and rounded corners.
 * It does not copy Microsoft assets, fonts or code.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF0067C0),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60CDFF),
    onPrimary = Color(0xFF003355),
    background = Color(0xFF202020),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF2B2B2B),
    onSurface = Color(0xFFFFFFFF),
)

private val WindowsLikeShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
)

@Composable
fun DroidExplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = WindowsLikeShapes,
        content = content,
    )
}
