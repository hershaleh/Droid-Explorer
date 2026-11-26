package com.droidexplorer.ui.design.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Original line icons drawn in the style of Windows 11 (thin 1dp strokes, rounded caps, 20x20 grid).
 * They are not copies of Segoe Fluent Icons or Fluent System Icons.
 * Directional icons mirror automatically in right-to-left layouts.
 */
enum class FluentIcons(val mirrorsInRtl: Boolean = false) {
    Back(true), Forward(true), Up, Refresh, Search, ChevronRight(true), ChevronDown, Close, Add,
    Cut, Copy, Paste, Rename, Share, Delete, Sort, View, List, More,
    Home, Download, Document, Picture, Music, Video, Drive, Cloud, Network, Settings,
}

private class Pen(val scope: DrawScope, val color: Color, val width: Float) {
    private val style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        scope.drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = width, cap = StrokeCap.Round)

    fun poly(vararg p: Float, close: Boolean = false) {
        val path = Path()
        path.moveTo(p[0], p[1])
        var i = 2
        while (i < p.size) {
            path.lineTo(p[i], p[i + 1])
            i += 2
        }
        if (close) path.close()
        scope.drawPath(path, color, style = style)
    }

    fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        val path = Path()
        path.addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + w, y + h, r, r))
        scope.drawPath(path, color, style = style)
    }

    fun circle(cx: Float, cy: Float, r: Float, fill: Boolean = false) {
        if (fill) {
            scope.drawCircle(color, r, Offset(cx, cy))
        } else {
            scope.drawCircle(color, r, Offset(cx, cy), style = style)
        }
    }

    fun path(build: Path.() -> Unit) {
        val path = Path()
        path.build()
        scope.drawPath(path, color, style = style)
    }

    fun arc(left: Float, top: Float, w: Float, h: Float, start: Float, sweep: Float) {
        scope.drawArc(color, start, sweep, false, Offset(left, top), Size(w, h), style = style)
    }
}

@Composable
fun FluentIcon(
    icon: FluentIcons,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 16.dp,
) {
    Canvas(modifier.size(size)) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val unit = this.size.width / 20f
        val flip = rtl && icon.mirrorsInRtl
        val strokeWidth = 1.2.dp.toPx() / unit
        scale(scale = unit, pivot = Offset.Zero) {
            if (flip) {
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(10f, 0f)) {
                    drawIcon(this, tint, icon, strokeWidth)
                }
            } else {
                drawIcon(this, tint, icon, strokeWidth)
            }
        }
    }
}

private fun drawIcon(scope: DrawScope, tint: Color, icon: FluentIcons, sw: Float) {
    val p = Pen(scope, tint, sw)
    when (icon) {
        FluentIcons.Back -> { p.line(16f, 10f, 4.5f, 10f); p.poly(9f, 5.5f, 4.5f, 10f, 9f, 14.5f) }
        FluentIcons.Forward -> { p.line(4f, 10f, 15.5f, 10f); p.poly(11f, 5.5f, 15.5f, 10f, 11f, 14.5f) }
        FluentIcons.Up -> { p.line(10f, 16f, 10f, 4.5f); p.poly(5.5f, 9f, 10f, 4.5f, 14.5f, 9f) }
        FluentIcons.Refresh -> {
            p.arc(4f, 4.5f, 12f, 12f, -40f, 290f)
            p.poly(14.8f, 2.8f, 14.8f, 6.4f, 11.2f, 6.4f)
        }
        FluentIcons.Search -> { p.circle(8.5f, 8.5f, 5f); p.line(12.3f, 12.3f, 16.5f, 16.5f) }
        FluentIcons.ChevronRight -> p.poly(7.5f, 4.5f, 13f, 10f, 7.5f, 15.5f)
        FluentIcons.ChevronDown -> p.poly(4.5f, 7.5f, 10f, 13f, 15.5f, 7.5f)
        FluentIcons.Close -> { p.line(5f, 5f, 15f, 15f); p.line(15f, 5f, 5f, 15f) }
        FluentIcons.Add -> { p.line(10f, 3.5f, 10f, 16.5f); p.line(3.5f, 10f, 16.5f, 10f) }
        FluentIcons.Cut -> {
            p.circle(6f, 15f, 2.3f); p.circle(14f, 15f, 2.3f)
            p.line(7.4f, 13.1f, 13f, 3.5f); p.line(12.6f, 13.1f, 7f, 3.5f)
        }
        FluentIcons.Copy -> { p.rrect(7f, 7f, 9.5f, 10f, 1.8f); p.path { moveTo(13f, 5f); lineTo(13f, 4.8f); quadraticTo(13f, 3f, 11.2f, 3f); lineTo(5.8f, 3f); quadraticTo(4f, 3f, 4f, 4.8f); lineTo(4f, 11.2f); quadraticTo(4f, 13f, 5.8f, 13f); lineTo(6f, 13f) } }
        FluentIcons.Paste -> {
            p.rrect(4f, 4f, 12f, 13.5f, 1.8f); p.rrect(7f, 2.5f, 6f, 3.2f, 1f)
            p.line(7f, 10f, 13f, 10f); p.line(7f, 13f, 11f, 13f)
        }
        FluentIcons.Rename -> {
            p.rrect(2.5f, 5.5f, 15f, 9f, 1.8f)
            p.line(10f, 8f, 10f, 12f); p.line(8.5f, 8f, 11.5f, 8f); p.line(8.5f, 12f, 11.5f, 12f)
        }
        FluentIcons.Share -> {
            p.poly(7f, 8.5f, 5f, 8.5f, 5f, 16.5f, 15f, 16.5f, 15f, 8.5f, 13f, 8.5f)
            p.line(10f, 12f, 10f, 3f); p.poly(6.8f, 6.2f, 10f, 3f, 13.2f, 6.2f)
        }
        FluentIcons.Delete -> {
            p.line(3.5f, 6f, 16.5f, 6f); p.poly(7.5f, 6f, 7.5f, 3.8f, 12.5f, 3.8f, 12.5f, 6f)
            p.poly(5f, 6f, 6f, 16.5f, 14f, 16.5f, 15f, 6f)
            p.line(8.5f, 9f, 8.5f, 14f); p.line(11.5f, 9f, 11.5f, 14f)
        }
        FluentIcons.Sort -> {
            p.line(6f, 16f, 6f, 4f); p.poly(3.2f, 6.8f, 6f, 4f, 8.8f, 6.8f)
            p.line(14f, 4f, 14f, 16f); p.poly(11.2f, 13.2f, 14f, 16f, 16.8f, 13.2f)
        }
        FluentIcons.View -> {
            p.rrect(3.5f, 3.5f, 5.5f, 5.5f, 1f); p.rrect(11f, 3.5f, 5.5f, 5.5f, 1f)
            p.rrect(3.5f, 11f, 5.5f, 5.5f, 1f); p.rrect(11f, 11f, 5.5f, 5.5f, 1f)
        }
        FluentIcons.List -> {
            p.line(8f, 5f, 16.5f, 5f); p.line(8f, 10f, 16.5f, 10f); p.line(8f, 15f, 16.5f, 15f)
            p.circle(4.5f, 5f, 0.9f, true); p.circle(4.5f, 10f, 0.9f, true); p.circle(4.5f, 15f, 0.9f, true)
        }
        FluentIcons.More -> { p.circle(4.5f, 10f, 1.3f, true); p.circle(10f, 10f, 1.3f, true); p.circle(15.5f, 10f, 1.3f, true) }
        FluentIcons.Home -> {
            p.poly(2.8f, 10.2f, 10f, 3.5f, 17.2f, 10.2f)
            p.poly(4.8f, 9f, 4.8f, 16.5f, 8.2f, 16.5f, 8.2f, 12f, 11.8f, 12f, 11.8f, 16.5f, 15.2f, 16.5f, 15.2f, 9f)
        }
        FluentIcons.Download -> { p.line(10f, 3.5f, 10f, 13f); p.poly(5.8f, 9f, 10f, 13.2f, 14.2f, 9f); p.line(4f, 16.5f, 16f, 16.5f) }
        FluentIcons.Document -> {
            p.poly(5f, 2.8f, 11.5f, 2.8f, 15f, 6.3f, 15f, 17.2f, 5f, 17.2f, close = true)
            p.poly(11.5f, 2.8f, 11.5f, 6.3f, 15f, 6.3f)
            p.line(7.5f, 10.5f, 12.5f, 10.5f); p.line(7.5f, 13.5f, 12.5f, 13.5f)
        }
        FluentIcons.Picture -> {
            p.rrect(3f, 4f, 14f, 12f, 1.8f); p.circle(7.5f, 8.5f, 1.4f)
            p.poly(3.5f, 14.5f, 8f, 10.5f, 11f, 13f, 13.5f, 11f, 16.5f, 14f)
        }
        FluentIcons.Music -> {
            p.circle(7f, 15f, 2.3f); p.circle(14f, 13f, 2.3f)
            p.poly(9.3f, 15f, 9.3f, 5f, 16.3f, 3.5f, 16.3f, 13f)
        }
        FluentIcons.Video -> { p.rrect(3f, 5f, 10.5f, 10f, 1.8f); p.poly(13.5f, 9f, 17f, 6.8f, 17f, 13.2f, 13.5f, 11f) }
        FluentIcons.Drive -> {
            p.rrect(2.5f, 10f, 15f, 6.5f, 1.8f); p.poly(4.5f, 10f, 6.5f, 4.5f, 13.5f, 4.5f, 15.5f, 10f)
            p.circle(14f, 13.2f, 0.9f, true)
        }
        FluentIcons.Cloud -> p.path {
            moveTo(6f, 15.5f)
            cubicTo(2.5f, 15.5f, 2.5f, 9.5f, 6.5f, 9.2f)
            cubicTo(7f, 5.5f, 13.5f, 5f, 14f, 9.6f)
            cubicTo(17.8f, 9.6f, 17.8f, 15.5f, 14.5f, 15.5f)
            close()
        }
        FluentIcons.Network -> { p.rrect(3f, 4f, 14f, 9f, 1.8f); p.line(8f, 16.5f, 12f, 16.5f); p.line(10f, 13f, 10f, 16.5f) }
        FluentIcons.Settings -> {
            p.circle(10f, 10f, 2.6f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val c = Math.cos(a).toFloat()
                val s = Math.sin(a).toFloat()
                p.line(10f + c * 5.6f, 10f + s * 5.6f, 10f + c * 7.4f, 10f + s * 7.4f)
            }
            p.circle(10f, 10f, 5.6f)
        }
    }
}
