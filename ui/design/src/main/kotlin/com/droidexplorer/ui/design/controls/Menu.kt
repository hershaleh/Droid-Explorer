package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

/** Windows 11 flyout menu: opaque flyout fill, 1dp surface stroke, 8dp corners. */
@Composable
fun FluentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = FluentTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 200.dp),
        containerColor = c.flyoutFill,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        shape = FluentShapes.overlay,
        border = BorderStroke(1.dp, c.surfaceStroke),
        content = content,
    )
}

@Composable
fun FluentMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    icon: FluentIcons? = null,
) {
    val c = FluentTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(36.dp)
            .clip(FluentShapes.control)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            val tint = if (enabled) c.textPrimary else c.textDisabled
            if (checked) FluentIcon(FluentIcons.Check, tint = tint, size = 14.dp)
            else if (icon != null) FluentIcon(icon, tint = tint, size = 16.dp)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = FluentTheme.typography.body, color = if (enabled) c.textPrimary else c.textDisabled)
    }
}

@Composable
fun FluentMenuHeader(text: String) {
    Text(
        text,
        style = FluentTheme.typography.caption,
        color = FluentTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun FluentMenuDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(FluentTheme.colors.divider))
}
