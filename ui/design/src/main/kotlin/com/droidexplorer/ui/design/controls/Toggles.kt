package com.droidexplorer.ui.design.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentTheme

/** Windows 11 toggle switch: 40x20 pill, 12dp knob, accent fill when on. */
@Composable
fun FluentToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = FluentTheme.colors
    val knobOffset = animateDpAsState(if (checked) 24.dp else 4.dp, tween(120), label = "knob")
    val trackFill = when {
        checked && enabled -> c.accent
        checked -> c.accentDisabled
        else -> c.controlFillSecondary
    }
    val knobColor = when {
        checked && enabled -> c.textOnAccent
        checked -> c.textOnAccentDisabled
        enabled -> c.textSecondary
        else -> c.textDisabled
    }
    var track = modifier
        .size(width = 40.dp, height = 20.dp)
        .background(trackFill, CircleShape)
    if (!checked) {
        track = track.border(1.dp, if (enabled) c.controlStrongStroke else c.controlStrongStrokeDisabled, CircleShape)
    }
    Box(
        modifier = track.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobOffset.value)
                .size(12.dp)
                .background(knobColor, CircleShape),
        )
    }
}

/** Windows 11 check box: 20dp box with 4dp corners, accent fill and a check glyph when checked. */
@Composable
fun FluentCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val c = FluentTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var box = Modifier.size(20.dp)
        box = if (checked) {
            box.background(if (enabled) c.accent else c.accentDisabled, shape)
        } else {
            box
                .background(c.controlFillSecondary, shape)
                .border(1.dp, if (enabled) c.controlStrongStroke else c.controlStrongStrokeDisabled, shape)
        }
        Canvas(box) {
            if (checked) {
                val s = size.width / 20f
                val p = Path().apply {
                    moveTo(5.2f * s, 10.4f * s)
                    lineTo(8.6f * s, 13.8f * s)
                    lineTo(14.8f * s, 6.6f * s)
                }
                drawPath(
                    path = p,
                    color = if (enabled) c.textOnAccent else c.textOnAccentDisabled,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = FluentTheme.typography.body,
                color = if (enabled) c.textPrimary else c.textDisabled,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
