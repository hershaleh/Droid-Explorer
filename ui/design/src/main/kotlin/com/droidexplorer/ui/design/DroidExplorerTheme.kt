package com.droidexplorer.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalFluentColors = staticCompositionLocalOf<FluentColors> { error("DroidExplorerTheme is missing") }
val LocalFluentTypography = staticCompositionLocalOf<FluentTypography> { error("DroidExplorerTheme is missing") }

/** Windows 11 corner radii: 4dp for controls, 8dp for overlays (flyouts, dialogs, windows). */
object FluentShapes {
    val control = RoundedCornerShape(4.dp)
    val overlay = RoundedCornerShape(8.dp)
}

object FluentTheme {
    val colors: FluentColors
        @Composable @ReadOnlyComposable get() = LocalFluentColors.current
    val typography: FluentTypography
        @Composable @ReadOnlyComposable get() = LocalFluentTypography.current
}

/**
 * Droid Explorer theme. All components read their look from [FluentTheme] (WinUI-style tokens).
 * A Material3 theme is also provided only so that stock Compose pieces (text defaults, ripple-less
 * surfaces) inherit sensible colours; Droid Explorer screens must not use Material components directly.
 */
@Composable
fun DroidExplorerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = remember(darkTheme) { if (darkTheme) darkFluentColors() else lightFluentColors() }
    val typography = remember { fluentTypography() }
    val materialColors = remember(colors) {
        if (darkTheme) {
            darkColorScheme(
                primary = colors.accent,
                onPrimary = colors.textOnAccent,
                background = colors.windowBackground,
                onBackground = colors.textPrimary,
                surface = colors.windowBackground,
                onSurface = colors.textPrimary,
            )
        } else {
            lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.textOnAccent,
                background = colors.windowBackground,
                onBackground = colors.textPrimary,
                surface = colors.windowBackground,
                onSurface = colors.textPrimary,
            )
        }
    }
    CompositionLocalProvider(
        LocalFluentColors provides colors,
        LocalFluentTypography provides typography,
    ) {
        MaterialTheme(colorScheme = materialColors) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textPrimary,
                LocalTextStyle provides typography.body,
                content = content,
            )
        }
    }
}
