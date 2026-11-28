package com.droidexplorer.feature.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridCell(item: ExplorerItem, mode: ViewMode, actions: ExplorerActions) {
    val c = FluentTheme.colors
    var menuOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        pressed -> c.textPrimary.copy(alpha = 0.14f)
        item.selected -> c.accent.copy(alpha = if (c.isDark) 0.22f else 0.14f)
        hovered -> c.subtleFillSecondary
        else -> Color.Transparent
    }
    val height = when (mode) {
        ViewMode.List -> 32.dp
        ViewMode.Tiles -> 60.dp
        else -> 112.dp
    }
    val name: @Composable (Modifier, Int, TextAlign) -> Unit = { mod, lines, align ->
        if (actions.renamingPath == item.path) {
            InlineRenameField(
                initial = item.name,
                isFolder = item.kind == ItemKind.Folder,
                onCommit = { actions.onRenameCommit(item.path, it) },
                onCancel = actions.onRenameCancel,
                modifier = mod,
                singleLine = lines == 1,
            )
        } else {
            Text(
                displayName(item, actions.hideExtensions),
                style = if (mode == ViewMode.Icons) FluentTheme.typography.caption else FluentTheme.typography.body,
                color = c.textPrimary,
                maxLines = lines,
                overflow = TextOverflow.Ellipsis,
                textAlign = align,
                modifier = mod,
            )
        }
    }
    Box(
        Modifier
            .padding(2.dp)
            .fillMaxWidth()
            .height(height)
            .clip(FluentShapes.control)
            .background(fill, FluentShapes.control)
            .combinedClickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = c.textPrimary),
                onClick = { actions.onOpen(item) },
                onLongClick = { if (item.selected) menuOpen = true else actions.onToggleSelect(item) },
            ),
    ) {
        ExplorerContextMenu(ContextKind.Item, menuOpen, { menuOpen = false }, actions)
        when (mode) {
            ViewMode.List -> Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FileGlyph(item.kind)
                Spacer(Modifier.width(8.dp))
                name(Modifier, 1, TextAlign.Start)
            }
            ViewMode.Tiles -> Row(Modifier.align(Alignment.CenterStart).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                FileGlyph(item.kind, glyphSize = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    name(Modifier, 1, TextAlign.Start)
                    Text(item.kind.typeLabel(), style = FluentTheme.typography.caption, color = c.textSecondary, maxLines = 1)
                    item.sizeKb?.let { Text(formatSizeKb(it), style = FluentTheme.typography.caption, color = c.textSecondary, maxLines = 1) }
                }
            }
            else -> Column(
                Modifier.align(Alignment.Center).padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FileGlyph(item.kind, glyphSize = 56.dp)
                name(Modifier, 2, TextAlign.Center)
            }
        }
    }
}

/** List, Tiles and Large icons views: a grid that fills the width, with the same groups and selection as Details. */
@Composable
fun ExplorerGridList(
    items: List<ExplorerItem>,
    mode: ViewMode,
    modifier: Modifier = Modifier,
    actions: ExplorerActions = ExplorerActions(),
    emptyText: String? = null,
    revealPath: String? = null,
    onRevealed: () -> Unit = {},
) {
    val state = rememberLazyGridState()
    val groupBy = actions.groupBy
    val groups = remember(items, groupBy) { groupItems(items, groupBy) }
    LaunchedEffect(revealPath, groups) {
        if (revealPath == null) return@LaunchedEffect
        val found = flatIndexOf(groups, revealPath)
        if (found >= 0) {
            if (state.layoutInfo.visibleItemsInfo.none { it.index == found }) state.animateScrollToItem(found)
            onRevealed()
        }
    }
    val min = when (mode) {
        ViewMode.List -> 220.dp
        ViewMode.Tiles -> 260.dp
        else -> 104.dp
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(min),
        modifier = modifier.fillMaxWidth().testTag("view-${mode.name.lowercase()}"),
        state = state,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        groups.forEach { (header, rows) ->
            if (header != null) {
                item(key = "group-${header.group.name}-${header.kind.name}", span = { GridItemSpan(maxLineSpan) }) {
                    GroupHeader(header, groupBy, rows.size)
                }
            }
            items(rows, key = { it.path }) { GridCell(it, mode, actions) }
        }
        if (items.isEmpty() && emptyText != null) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(emptyText, style = FluentTheme.typography.body, color = FluentTheme.colors.textSecondary, modifier = Modifier.fillMaxWidth().padding(24.dp))
            }
        }
    }
}
