package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun ViewToggle(icon: FluentIcons, active: Boolean) {
    val c = FluentTheme.colors
    Box(
        Modifier
            .size(28.dp)
            .background(if (active) c.subtleFillSecondary else androidx.compose.ui.graphics.Color.Transparent, FluentShapes.control),
        contentAlignment = Alignment.Center,
    ) {
        FluentIcon(icon, size = 14.dp)
    }
}

/** Status bar: item count, selection summary and the Details/Tiles view toggle at the end. */
@Composable
fun ExplorerStatusBar(items: List<ExplorerItem>, modifier: Modifier = Modifier) {
    val c = FluentTheme.colors
    val selected = items.filter { it.selected }
    val total = if (items.size == 1) {
        stringResource(R.string.status_items_one)
    } else {
        stringResource(R.string.status_items_many, items.size)
    }
    Row(
        modifier = modifier.fillMaxWidth().height(32.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(total, style = FluentTheme.typography.caption, color = c.textPrimary)
        if (selected.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 10.dp).width(1.dp).height(14.dp).background(c.controlStrokeSecondary))
            val label = if (selected.size == 1) {
                stringResource(R.string.status_selected_one)
            } else {
                stringResource(R.string.status_selected_many, selected.size)
            }
            val sum = selected.sumOf { it.sizeKb ?: 0L }
            Text(label, style = FluentTheme.typography.caption, color = c.textPrimary)
            Spacer(Modifier.width(12.dp))
            Text(
                formatTotal(sum),
                style = FluentTheme.typography.caption.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        ViewToggle(FluentIcons.List, active = true)
        Spacer(Modifier.width(2.dp))
        ViewToggle(FluentIcons.View, active = false)
    }
}
