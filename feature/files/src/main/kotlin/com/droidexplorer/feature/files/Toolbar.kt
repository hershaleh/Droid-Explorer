package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
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
import com.droidexplorer.ui.design.controls.FluentDropdownMenu
import com.droidexplorer.ui.design.controls.FluentMenuDivider
import com.droidexplorer.ui.design.controls.FluentMenuHeader
import com.droidexplorer.ui.design.controls.FluentMenuItem
import com.droidexplorer.ui.design.controls.FluentTextBox
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun ToolbarIconButton(icon: FluentIcons, enabled: Boolean = true, label: String? = null, onClick: () -> Unit = {}) {
    FluentButton(
        onClick = onClick,
        style = ButtonStyle.Subtle,
        enabled = enabled,
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.size(32.dp).then(
            if (label != null) Modifier.semantics { contentDescription = label } else Modifier,
        ),
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
    var searchOpen by remember { mutableStateOf(false) }
    val hint = actions.searchHint ?: stringResource(R.string.search_hint)
    if (searchOpen && !showSearchBox) {
        // Phone: the search box takes over the whole row until it is closed.
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToolbarIconButton(FluentIcons.Close, true, stringResource(R.string.close)) {
                searchOpen = false
                actions.onQuery("")
            }
            FluentTextBox(
                value = actions.query,
                onValueChange = actions.onQuery,
                placeholder = hint,
                modifier = Modifier.weight(1f).focusRequester(focus).testTag("search-input"),
                trailing = { FluentIcon(FluentIcons.Search, tint = FluentTheme.colors.textSecondary) },
            )
        }
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        actions.onMenu?.let { ToolbarIconButton(FluentIcons.Menu, true, stringResource(R.string.menu), it) }
        ToolbarIconButton(FluentIcons.Back, actions.canBack, stringResource(R.string.back), actions.onBack)
        ToolbarIconButton(FluentIcons.Forward, actions.canForward, stringResource(R.string.forward), actions.onForward)
        ToolbarIconButton(FluentIcons.Up, actions.canUp, stringResource(R.string.up), actions.onUp)
        ToolbarIconButton(FluentIcons.Refresh, true, stringResource(R.string.refresh), actions.onRefresh)
        Spacer(Modifier.width(4.dp))
        AddressBar(Modifier.weight(1f), crumbs, actions.onCrumb, actions.editablePath, actions.onPathSubmit)
        Spacer(Modifier.width(6.dp))
        if (showSearchBox) {
            FluentTextBox(
                value = actions.query,
                onValueChange = actions.onQuery,
                placeholder = hint,
                modifier = Modifier.width(280.dp).testTag("search-input"),
                trailing = { FluentIcon(FluentIcons.Search, tint = FluentTheme.colors.textSecondary) },
            )
        } else {
            ToolbarIconButton(FluentIcons.Search, true, stringResource(R.string.search)) { searchOpen = true }
        }
    }
}

/**
 * Windows 11 address bar: folder glyph, breadcrumb segments separated by chevrons, history chevron.
 * Tapping the chevron (or the empty space) switches to a text box with the full path, as in Windows.
 */
@Composable
fun AddressBar(
    modifier: Modifier = Modifier,
    crumbs: List<String>? = null,
    onCrumb: (Int) -> Unit = {},
    editablePath: String? = null,
    onPathSubmit: (String) -> Unit = {},
) {
    val c = FluentTheme.colors
    val segments = crumbs ?: listOf(
        stringResource(R.string.addr_this_device),
        stringResource(R.string.addr_internal),
        stringResource(R.string.nav_downloads),
    )
    var editing by remember { mutableStateOf(false) }
    val canEdit = editablePath != null
    if (editing && editablePath != null) {
        // The whole path starts selected (blue) so Delete or typing replaces it; a second tap edits normally.
        var text by remember(editablePath) { mutableStateOf(TextFieldValue(editablePath, TextRange(0, editablePath.length))) }
        val focus = remember { FocusRequester() }
        var hadFocus by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { focus.requestFocus() }
        FluentTextBox(
            value = text,
            onValueChange = { text = it },
            modifier = modifier
                .focusRequester(focus)
                .onFocusChanged { st ->
                    if (st.isFocused) hadFocus = true else if (hadFocus) editing = false
                }
                .testTag("address-input"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Uri),
            keyboardActions = KeyboardActions(onGo = { editing = false; onPathSubmit(text.text.trim()) }),
            leading = { FolderGlyph() },
        )
        return
    }
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(FluentShapes.control)
            .background(c.controlFillInput, FluentShapes.control)
            .border(1.dp, c.controlStroke, FluentShapes.control)
            .then(if (canEdit) Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { editing = true } else Modifier)
            .padding(horizontal = 8.dp)
            .testTag("address-bar"),
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = androidx.compose.material3.ripple(color = c.textPrimary),
                        ) { onCrumb(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("crumb-$i"),
                ) {
                    Text(label, style = FluentTheme.typography.body, color = c.textPrimary, maxLines = 1)
                }
            }
        }
        val editLabel = stringResource(R.string.address_edit)
        Box(
            Modifier
                .size(width = 28.dp, height = 32.dp)
                .clickable(enabled = canEdit) { editing = true }
                .semantics { contentDescription = editLabel }
                .testTag("address-edit"),
            contentAlignment = Alignment.Center,
        ) {
            FluentIcon(FluentIcons.ChevronDown, tint = c.textSecondary, size = 10.dp)
        }
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
private fun CommandIcon(icon: FluentIcons, enabled: Boolean = true, label: String? = null, onClick: () -> Unit = {}) {
    ToolbarIconButton(icon, enabled, label, onClick)
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
        FluentDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FluentMenuHeader(stringResource(R.string.menu_sort_by))
            val keys = listOf(
                SortKey.NAME to R.string.col_name,
                SortKey.MODIFIED to R.string.col_modified,
                SortKey.TYPE to R.string.col_type,
                SortKey.SIZE to R.string.col_size,
            )
            keys.forEach { (key, label) ->
                FluentMenuItem(
                    text = stringResource(label),
                    checked = actions.sortKey == key,
                    onClick = { open = false; actions.onSort(key, actions.sortAscending) },
                )
            }
            FluentMenuDivider()
            FluentMenuItem(stringResource(R.string.sort_asc), { open = false; actions.onSort(actions.sortKey, true) }, checked = actions.sortAscending)
            FluentMenuItem(stringResource(R.string.sort_desc), { open = false; actions.onSort(actions.sortKey, false) }, checked = !actions.sortAscending)
            FluentMenuDivider()
            FluentMenuHeader(stringResource(R.string.menu_group_by))
            FluentMenuItem(stringResource(R.string.group_none), { open = false; actions.onGroupBy(GroupBy.None) }, checked = actions.groupBy == GroupBy.None)
            FluentMenuItem(stringResource(R.string.col_modified), { open = false; actions.onGroupBy(GroupBy.Date) }, checked = actions.groupBy == GroupBy.Date)
            FluentMenuItem(stringResource(R.string.col_type), { open = false; actions.onGroupBy(GroupBy.Type) }, checked = actions.groupBy == GroupBy.Type)
        }
    }
}

@Composable
private fun ViewMenu(actions: ExplorerActions, showLabel: Boolean) {
    var open by remember { mutableStateOf(false) }
    Box {
        CommandLabeled(FluentIcons.View, stringResource(R.string.cmd_view), showLabel, chevron = true) { open = true }
        FluentDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                ViewMode.Icons to R.string.view_icons,
                ViewMode.List to R.string.view_list,
                ViewMode.Details to R.string.view_details,
                ViewMode.Tiles to R.string.view_tiles,
            ).forEach { (mode, label) ->
                FluentMenuItem(stringResource(label), { open = false; actions.onViewMode(mode) }, checked = actions.viewMode == mode)
            }
            FluentMenuDivider()
            FluentMenuItem(
                stringResource(R.string.show_hidden),
                { open = false; actions.onShowHidden(!actions.showHidden) },
                checked = actions.showHidden,
            )
        }
    }
}

@Composable
private fun MoreMenu(actions: ExplorerActions) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.cmd_more)
    Box {
        ToolbarIconButton(FluentIcons.More, true, label) { open = true }
        FluentDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FluentMenuItem(stringResource(R.string.select_all), { open = false; actions.onSelectAll() })
            FluentMenuItem(stringResource(R.string.select_none), { open = false; actions.onSelectNone() })
            FluentMenuItem(stringResource(R.string.select_invert), { open = false; actions.onInvertSelection() })
        }
    }
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
        CommandIcon(FluentIcons.Cut, hasSelection, stringResource(R.string.cmd_cut), actions.onCut)
        CommandIcon(FluentIcons.Copy, hasSelection, stringResource(R.string.cmd_copy), actions.onCopy)
        CommandIcon(FluentIcons.Paste, actions.canPaste, stringResource(R.string.cmd_paste), actions.onPaste)
        CommandIcon(FluentIcons.Rename, singleSelection && hasSelection, stringResource(R.string.cmd_rename), actions.onRename)
        CommandIcon(FluentIcons.Share, hasSelection, stringResource(R.string.cmd_share), actions.onShare)
        CommandIcon(FluentIcons.Delete, hasSelection, stringResource(R.string.cmd_delete), actions.onDelete)
        CommandDivider()
        SortMenu(actions, showLabel = !compact)
        ViewMenu(actions, showLabel = !compact)
        CommandDivider()
        MoreMenu(actions)
    }
}
