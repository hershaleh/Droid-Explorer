package com.droidexplorer.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color

/** Gray tint shown while a finger is down on a row, button or crumb. Visible on touch, where there is no hover. */
val FluentColors.pressFill: Color get() = textPrimary.copy(alpha = if (isDark) 0.16f else 0.11f)

/**
 * Background colour for a touchable item: [base] normally, [pressFill] while pressed, with a short fade
 * like the Windows 11 press animation.
 */
@Composable
fun pressedBackground(interaction: InteractionSource, base: Color = Color.Transparent): State<Color> {
    val pressed = interaction.collectIsPressedAsState().value
    val target = if (pressed) FluentTheme.colors.pressFill else base
    return animateColorAsState(target, tween(if (pressed) 40 else 140), label = "press")
}
