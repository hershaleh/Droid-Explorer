package com.droidexplorer.feature.files

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentTheme

/** Original folder glyph in the Windows 11 manner: darker back with a tab, lighter front panel. */
@Composable
fun FolderGlyph(modifier: Modifier = Modifier, glyphSize: Dp = 16.dp) {
    Canvas(modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val r = CornerRadius(w * 0.08f)
        drawRoundRect(Color(0xFFE0A100), Offset(w * 0.04f, h * 0.14f), Size(w * 0.42f, h * 0.3f), r)
        drawRoundRect(Color(0xFFEDB100), Offset(w * 0.04f, h * 0.24f), Size(w * 0.92f, h * 0.6f), r)
        drawRoundRect(Color(0xFFFFD04F), Offset(w * 0.04f, h * 0.36f), Size(w * 0.92f, h * 0.5f), r)
    }
}

private fun ItemKind.badgeColor(): Color = when (this) {
    ItemKind.Pdf -> Color(0xFFD13438)
    ItemKind.Zip -> Color(0xFFC19C00)
    ItemKind.Image -> Color(0xFF0F7B0F)
    ItemKind.Audio -> Color(0xFF8764B8)
    ItemKind.Video -> Color(0xFF0078D4)
    ItemKind.Document -> Color(0xFF2B579A)
    ItemKind.Apk -> Color(0xFF2E8B57)
    ItemKind.Text -> Color(0xFF767676)
    ItemKind.Folder -> Color.Transparent
}

/** Original file glyph: a page with a folded corner and a coloured band that identifies the type. */
@Composable
fun FileGlyph(kind: ItemKind, modifier: Modifier = Modifier, glyphSize: Dp = 16.dp) {
    if (kind == ItemKind.Folder) {
        FolderGlyph(modifier, glyphSize)
        return
    }
    val dark = FluentTheme.colors.isDark
    val badge = kind.badgeColor()
    Canvas(modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val left = w * 0.2f
        val right = w * 0.8f
        val top = h * 0.06f
        val bottom = h * 0.94f
        val fold = w * 0.22f
        val page = Path().apply {
            moveTo(left, top)
            lineTo(right - fold, top)
            lineTo(right, top + fold)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(page, if (dark) Color(0xFFEDEDED) else Color(0xFFFFFFFF))
        drawPath(page, Color(0xFF9A9A9A), style = Stroke(width = w * 0.06f, join = StrokeJoin.Round))
        drawRect(badge, Offset(left, h * 0.52f), Size(right - left, h * 0.22f))
    }
}
