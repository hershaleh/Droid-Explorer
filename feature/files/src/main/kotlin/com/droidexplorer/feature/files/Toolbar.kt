package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidexplorer.storage.api.SortKey
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ButtonStyle
import com.droidexplorer.ui.design.controls.FluentButton
import com.droidexplorer.ui.design.controls.FluentTextBox
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun ToolbarIconButton(icon: FluentIcons, enabled: Boolean = true, onClick: () -> Unit = {}) {
    FluentButton(
        onClick = onClick,
        style = ButtonStyle.Subtle,
        enabled = enabled,
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.size(32.dp),
    ) {
        FluentIcon(icon, size = 16.dp)
    }
}

/** Back, forward, up and refresh buttons, the breadcrumb address bar and the search box. */
@Composable
fun ExplorerToolbar(
    showSearchBox: Boolean,
    modifier: Modifier = Modifier,
    actions: ExplorerActions = ExplorerActions(),
    crumbs: List<String>? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToolbarIconButton(FluentIcons.Back, actions.canBack, actions.onBack)
        ToolbarIconButton(FluentIcons.Forward, actions.canForward, actions.onForward)
        ToolbarIconButton(FluentIcons.Up, actions.canUp, actions.onUp)
        ToolbarIconButton(FluentIcons.Refresh, true, actions.onRefresh)
        Spacer(Modifier.width(4.dp))
        AddressBar(Modifier.weight(1f), crumbs, actions.onCrumb)
        Spacer(Modifier.width(6.dp))
        if (showSearchBox) {
            var query by remember { mutableStateOf("") }
            FluentTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier.width(280.dp),
                trailing = { FluentIcon(FluentIcons.Search, tint = FluentTheme.colors.textSecondary) },
            )
        } else {
            ToolbarIconButton(FluentIcons.Search)
        }
    }
}

/** Windows 11 address bar: folder glyph, breadcrumb segments separated by chevrons, history chevron. */
@Composable
fun AddressBar(modifier: Modifier = Modifier, crumbs: List<String>? = null, onCrumb: (Int) -> Unit = {}) {
    val c = FluentTheme.colors
    val segments = crumbs ?: listOf(
        stringResource(R.string.addr_this_device),
        stringResource(R.string.addr_internal),
        stringResource(R.string.nav_downloads),
    )
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(FluentShapes.control)
            .background(c.controlFillInput, FluentShapes.control)
            .border(1.dp, c.controlStroke, FluentShapes.control)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderGlyph()
        Spacer(Modifier.width(4.dp))
        val crumbScroll = rememberScrollState()
        // Keep the current folder (the last crumb) visible, as Windows does when the path is long.
        LaunchedEffect(crumbScroll.maxValue) { crumbScroll.scrollTo(crumbScroll.maxValue) }
        Row(
            Modifier.weight(1f).horizontalScroll(crumbScroll),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEachIndexed { i, label ->
                if (i > 0) FluentIcon(FluentIcons.ChevronRight, tint = c.textSecondary, size = 10.dp)
                Box(
                    Modifier.clip(FluentShapes.control)
                        .clickable { onCrumb(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(label, style = FluentTheme.typography.body, color = c.textPrimary, maxLines = 1)
                }
            }
        }
        FluentIcon(FluentIcons.ChevronDown, tint = c.textSecondary, size = 10.dp)
    }
}

@Composable
private fun CommandDivider() {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(20.dp)
            .background(FluentTheme.colors.controlStrokeSecondary),
    )
}

@Composable
private fun CommandIcon(icon: FluentIcons, enabled: Boolean = true, onClick: () -> Unit = {}) {
    ToolbarIconButton(icon, enabled, onClick)
}

@Composable
private fun CommandLabeled(
    icon: FluentIcons,
    label: String,
    showLabel: Boolean,
    chevron: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    FluentButton(
        onClick = onClick,
        enabled = enabled,
        style = ButtonStyle.Subtle,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
    ) {
        FluentIcon(icon, size = 16.dp)
        if (showLabel) Text(label, style = FluentTheme.typography.body)
        if (chevron) FluentIcon(FluentIcons.ChevronDown, size = 10.dp)
    }
}

@Composable
private fun SortMenu(actions: ExplorerActions, showLabel: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box {
        CommandLabeled(FluentIcons.Sort, stringResource(R.string.cmd_sort), showLabel, chevron = true) { open = true }
        val c = FluentTheme.colors
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.layer,
            shape = FluentShapes.overlay,
        ) {
            val keys = listOf(
                SortKey.NAME to R.string.col_name,
                SortKey.MODIFIED to R.string.col_modified,
                SortKey.TYPE to R.string.col_type,
                SortKey.SIZE to R.string.col_size,
            )
            keys.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { MenuText(stringResource(label), checked = actions.sortKey == key) },
                    onClick = { open = false; actions.onSort(key, actions.sortAscending) },
                )
            }
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.sort_asc), checked = actions.sortAscending) },
                onClick = { open = false; actions.onSort(actions.sortKey, true) },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.sort_desc), checked = !actions.sortAscending) },
                onClick = { open = false; actions.onSort(actions.sortKey, false) },
            )
        }
    }
}

@Composable
private fun MenuText(text: String, checked: Boolean) {
    Text(
        (if (checked) "\u2713  " else "\u2003 ") + text,
        style = FluentTheme.typography.body,
        color = FluentTheme.colors.textPrimary,
    )
}

/**
 * Windows 11 command bar: "New" with label, icon-only Cut/Copy/Paste/Rename/Share/Delete,
 * then Sort and View, then "See more". Selection-dependent commands are disabled with no selection.
 * View and See-more stay disabled until their menus exist.
 */
@Composable
fun ExplorerCommandBar(
    hasSelection: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    actions: ExplorerActions = ExplorerActions(),
    singleSelection: Boolean = hasSelection,
    interactive: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CommandLabeled(FluentIcons.Add, stringResource(R.string.cmd_new), showLabel = true, chevron = false, enabled = interactive, onClick = actions.onNewFolder)
        CommandDivider()
        CommandIcon(FluentIcons.Cut, hasSelection, actions.onCut)
        CommandIcon(FluentIcons.Copy, hasSelection, actions.onCopy)
        CommandIcon(FluentIcons.Paste, actions.canPaste, actions.onPaste)
        CommandIcon(FluentIcons.Rename, singleSelection && hasSelection, actions.onRename)
        CommandIcon(FluentIcons.Share, hasSelection, actions.onShare)
        CommandIcon(FluentIcons.Delete, hasSelection, actions.onDelete)
        CommandDivider()
        SortMenu(actions, showLabel = !compact)
        CommandLabeled(FluentIcons.View, stringResource(R.string.cmd_view), showLabel = !compact, chevron = true, enabled = false)
        CommandDivider()
        CommandIcon(FluentIcons.More, enabled = false)
    }
}
