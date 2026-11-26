package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

enum class ButtonStyle { Standard, Accent, Subtle }

/**
 * Windows 11 button: 32dp tall, 4dp corners, 11/5dp padding, elevated 1dp border on standard buttons
 * (lighter at the top, darker at the bottom), accent fill for the primary action, borderless subtle style.
 */
@Composable
fun FluentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Standard,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 11.dp, vertical = 5.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val c = FluentTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    val background: Color
    val foreground: Color
    val borderBrush: Brush?
    when (style) {
        ButtonStyle.Standard -> {
            background = when {
                !enabled -> c.controlFillDisabled
                pressed -> c.controlFillTertiary
                hovered -> c.controlFillSecondary
                else -> c.controlFill
            }
            foreground = when {
                !enabled -> c.textDisabled
                pressed -> c.textSecondary
                else -> c.textPrimary
            }
            borderBrush = if (enabled && !pressed) {
                Brush.verticalGradient(0f to c.controlStroke, 0.7f to c.controlStroke, 1f to c.controlStrokeSecondary)
            } else {
                Brush.verticalGradient(listOf(c.controlStroke, c.controlStroke))
            }
        }
        ButtonStyle.Accent -> {
            background = when {
                !enabled -> c.accentDisabled
                pressed -> c.accentTertiary
                hovered -> c.accentSecondary
                else -> c.accent
            }
            foreground = when {
                !enabled -> c.textOnAccentDisabled
                pressed -> c.textOnAccent.copy(alpha = 0.7f)
                else -> c.textOnAccent
            }
            borderBrush = if (enabled && !pressed) {
                Brush.verticalGradient(listOf(c.controlStrokeOnAccent, c.controlStrokeOnAccentSecondary))
            } else {
                null
            }
        }
        ButtonStyle.Subtle -> {
            background = when {
                !enabled -> Color.Transparent
                pressed -> c.subtleFillTertiary
                hovered -> c.subtleFillSecondary
                else -> Color.Transparent
            }
            foreground = when {
                !enabled -> c.textDisabled
                pressed -> c.textSecondary
                else -> c.textPrimary
            }
            borderBrush = null
        }
    }

    val shape = FluentShapes.control
    var m = modifier
        .defaultMinSize(minHeight = 32.dp)
        .clip(shape)
        .background(background, shape)
    if (borderBrush != null) m = m.border(1.dp, borderBrush, shape)
    if (focused) m = m.border(2.dp, c.focusOuter, shape)
    m = m
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
        .padding(contentPadding)

    CompositionLocalProvider(
        LocalContentColor provides foreground,
        LocalTextStyle provides FluentTheme.typography.body,
    ) {
        Row(
            modifier = m,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
