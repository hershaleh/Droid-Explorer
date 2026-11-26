package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

enum class ExplorerLayout { Wide, Medium, Narrow }

private data class Columns(val modified: Dp?, val type: Dp?, val size: Dp?)

private fun ExplorerLayout.columns(): Columns = when (this) {
    ExplorerLayout.Wide -> Columns(modified = 170.dp, type = 140.dp, size = 100.dp)
    ExplorerLayout.Medium -> Columns(modified = 150.dp, type = null, size = 90.dp)
    ExplorerLayout.Narrow -> Columns(modified = 130.dp, type = null, size = null)
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, sorted: Boolean = false, alignEnd: Boolean = false) {
    val c = FluentTheme.colors
    Row(modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = FluentTheme.typography.body,
            color = c.textPrimary,
            maxLines = 1,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (sorted) FluentIcon(FluentIcons.Up, tint = c.textSecondary, size = 10.dp)
        Spacer(Modifier.width(6.dp))
        Box(Modifier.width(1.dp).height(20.dp).background(c.controlStrokeSecondary))
    }
}

@Composable
private fun ColumnHeader(layout: ExplorerLayout) {
    val cols = layout.columns()
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        HeaderCell(stringResource(R.string.col_name), Modifier.weight(1f), sorted = true)
        cols.modified?.let { HeaderCell(stringResource(R.string.col_modified), Modifier.width(it)) }
        cols.type?.let { HeaderCell(stringResource(R.string.col_type), Modifier.width(it)) }
        cols.size?.let { HeaderCell(stringResource(R.string.col_size), Modifier.width(it), alignEnd = true) }
    }
}

@Composable
private fun GroupHeader(group: DateGroup, count: Int) {
    val c = FluentTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FluentIcon(FluentIcons.ChevronDown, tint = c.textSecondary, size = 10.dp)
        Spacer(Modifier.width(8.dp))
        Text("${group.label()} ($count)", style = FluentTheme.typography.bodyStrong, color = c.textPrimary)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(c.controlStrokeSecondary))
    }
}

@Composable
private fun ItemRow(item: ExplorerItem, layout: ExplorerLayout) {
    val c = FluentTheme.colors
    val cols = layout.columns()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val fill = when {
        item.selected -> c.accent.copy(alpha = if (c.isDark) 0.22f else 0.14f)
        hovered -> c.subtleFillSecondary
        else -> Color.Transparent
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .height(32.dp)
            .clip(FluentShapes.control)
            .background(fill, FluentShapes.control)
            .clickable(interactionSource = interaction, indication = null, onClick = {}),
    ) {
        if (item.selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(16.dp)
                    .background(c.accent, RoundedCornerShape(2.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FileGlyph(item.kind)
                Spacer(Modifier.width(10.dp))
                Text(
                    item.name,
                    style = FluentTheme.typography.body,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            cols.modified?.let {
                Text(
                    item.modified,
                    style = FluentTheme.typography.body,
                    color = c.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.width(it).padding(horizontal = 8.dp),
                )
            }
            cols.type?.let {
                Text(
                    item.kind.typeLabel(),
                    style = FluentTheme.typography.body,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(it).padding(horizontal = 8.dp),
                )
            }
            cols.size?.let {
                Text(
                    item.sizeKb?.let(::formatSizeKb).orEmpty(),
                    style = FluentTheme.typography.body,
                    color = c.textPrimary,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(it).padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/** Details view: sortable column header, date groups with a rule line, and rows with a selection pill. */
@Composable
fun ExplorerDetailsList(items: List<ExplorerItem>, layout: ExplorerLayout, modifier: Modifier = Modifier) {
    val groups = DateGroup.values().map { g -> g to items.filter { it.group == g } }.filter { it.second.isNotEmpty() }
    androidx.compose.foundation.layout.Column(modifier) {
        ColumnHeader(layout)
        LazyColumn(Modifier.weight(1f)) {
            groups.forEach { (group, rows) ->
                item(key = "group-${group.name}") { GroupHeader(group, rows.size) }
                items(rows, key = { it.name }) { ItemRow(it, layout) }
            }
        }
    }
}
