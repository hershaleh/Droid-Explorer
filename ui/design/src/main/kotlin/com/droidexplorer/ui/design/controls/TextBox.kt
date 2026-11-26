package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

/**
 * Windows 11 text box: 32dp tall, 4dp corners, 1dp border, a strong 1dp bottom line that becomes a
 * 2dp accent line while focused, and a slightly more opaque fill when focused.
 */
@Composable
fun FluentTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val c = FluentTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = FluentShapes.control

    val fill = when {
        !enabled -> c.controlFillDisabled
        focused -> c.controlFillInputActive
        hovered -> c.controlFillSecondary
        else -> c.controlFillInput
    }
    val bottomColor = when {
        !enabled -> c.controlStrongStrokeDisabled
        focused -> c.accent
        else -> c.controlStrongStroke
    }
    val bottomHeight = if (focused) 2.dp else 1.dp
    val textStyle = FluentTheme.typography.body.copy(color = if (enabled) c.textPrimary else c.textDisabled)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = textStyle,
        cursorBrush = SolidColor(c.textPrimary),
        interactionSource = interaction,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .clip(shape)
                    .background(fill, shape)
                    .border(1.dp, c.controlStroke, shape)
                    .drawBehind {
                        val h = bottomHeight.toPx()
                        drawRect(bottomColor, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    Box(Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, style = textStyle, color = c.textSecondary)
                    }
                    inner()
                }
                if (trailing != null) {
                    Box(Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        },
    )
}
