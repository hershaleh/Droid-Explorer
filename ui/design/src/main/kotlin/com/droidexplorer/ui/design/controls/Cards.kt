package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

/** Plain Windows 11 card: translucent fill, 1dp stroke, 4dp corners. */
@Composable
fun FluentCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = FluentTheme.colors
    Column(
        modifier = modifier
            .background(c.cardFill, FluentShapes.control)
            .border(1.dp, c.cardStroke, FluentShapes.control),
        content = content,
    )
}

/**
 * Row card as used in the Windows 11 Settings app: optional leading icon, title with a secondary
 * description underneath, and a control at the end. Minimum height 68dp, 16dp padding.
 */
@Composable
fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = FluentTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .background(c.cardFill, FluentShapes.control)
            .border(1.dp, c.cardStroke, FluentShapes.control)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.padding(end = 16.dp)) { icon() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = FluentTheme.typography.body, color = c.textPrimary)
            if (description != null) {
                Text(description, style = FluentTheme.typography.caption, color = c.textSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.size(16.dp))
            trailing()
        }
    }
}

enum class InfoBarSeverity { Informational, Success, Warning, Error }

/** Windows 11 InfoBar: tinted background, round severity glyph, bold title and message. */
@Composable
fun FluentInfoBar(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    severity: InfoBarSeverity = InfoBarSeverity.Informational,
) {
    val c = FluentTheme.colors
    val (fill, accent, glyph) = when (severity) {
        InfoBarSeverity.Informational -> Triple(c.attentionBackground, c.attention, "i")
        InfoBarSeverity.Success -> Triple(c.successBackground, c.success, "✓")
        InfoBarSeverity.Warning -> Triple(c.cautionBackground, c.caution, "!")
        InfoBarSeverity.Error -> Triple(c.criticalBackground, c.critical, "×")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(fill, FluentShapes.control)
            .border(1.dp, c.cardStroke, FluentShapes.control)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.padding(top = 2.dp).size(16.dp).background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                glyph,
                color = if (c.isDark) Color.Black else Color.White,
                style = FluentTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = FluentTheme.typography.bodyStrong, color = c.textPrimary)
            Text(message, style = FluentTheme.typography.body, color = c.textPrimary)
        }
    }
}
