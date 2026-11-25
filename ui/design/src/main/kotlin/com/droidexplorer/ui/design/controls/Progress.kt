package com.droidexplorer.ui.design.controls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentTheme

/**
 * Windows 11 progress bar: 1dp track line with a 3dp rounded accent indicator.
 * [progress] null means indeterminate.
 */
@Composable
fun FluentProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    val c = FluentTheme.colors
    val phase = if (progress == null) {
        val t = rememberInfiniteTransition(label = "bar")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        ).value
    } else {
        0f
    }
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val trackH = 1.dp.toPx()
        drawRect(c.controlStrongStroke, topLeft = Offset(0f, (size.height - trackH) / 2f), size = Size(size.width, trackH))
        val barH = 3.dp.toPx()
        val (start, width) = if (progress != null) {
            0f to size.width * progress.coerceIn(0f, 1f)
        } else {
            val w = size.width * 0.4f
            (-w + (size.width + w) * phase) to w
        }
        val x = if (rtl) size.width - start - width else start
        if (width > 0f) {
            drawRoundRect(c.accent, topLeft = Offset(x, 0f), size = Size(width, barH), cornerRadius = CornerRadius(barH / 2f))
        }
    }
}

/** Windows 11 indeterminate progress ring: 32dp, 4dp stroke, accent colour. */
@Composable
fun FluentProgressRing(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val c = FluentTheme.colors
    val t = rememberInfiniteTransition(label = "ring")
    val angle = t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    ).value
    Canvas(modifier.size(size)) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2f
        rotate(angle) {
            drawArc(
                color = c.accent,
                startAngle = -90f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - stroke, this.size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
