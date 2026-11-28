package com.droidexplorer.feature.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.droidexplorer.storage.api.SortKey
import com.droidexplorer.ui.design.controls.FluentDropdownMenu
import com.droidexplorer.ui.design.controls.FluentMenuDivider
import com.droidexplorer.ui.design.controls.FluentMenuItem
import com.droidexplorer.ui.design.icons.FluentIcons

enum class ContextKind { EmptyArea, Item, More }

private enum class Page { Main, View, Sort, Group }

/**
 * The File Explorer right-click menus, reached on touch by a long press: on empty space ([ContextKind.EmptyArea]),
 * or a second long press on a selected item ([ContextKind.Item]). [ContextKind.More] is the "..." button: the
 * parts of both menus that are not already on the command bar. Only commands that work on Android are shown.
 */
@Composable
fun ExplorerContextMenu(kind: ContextKind, expanded: Boolean, onDismiss: () -> Unit, actions: ExplorerActions) {
    var page by remember(expanded) { mutableStateOf(Page.Main) }
    val hasSel = actions.selectedCount > 0
    val single = actions.selectedCount == 1
    fun run(block: () -> Unit): () -> Unit = { onDismiss(); block() }

    FluentDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        when (page) {
            Page.View -> {
                SubHeader(R.string.cmd_view) { page = Page.Main }
                listOf(
                    ViewMode.Icons to R.string.view_icons, ViewMode.List to R.string.view_list,
                    ViewMode.Details to R.string.view_details, ViewMode.Tiles to R.string.view_tiles,
                ).forEach { (m, label) ->
                    FluentMenuItem(stringResource(label), run { actions.onViewMode(m) }, checked = actions.viewMode == m)
                }
            }
            Page.Sort -> {
                SubHeader(R.string.menu_sort_by) { page = Page.Main }
                listOf(SortKey.NAME to R.string.col_name, SortKey.MODIFIED to R.string.col_modified, SortKey.TYPE to R.string.col_type, SortKey.SIZE to R.string.col_size)
                    .forEach { (k, label) -> FluentMenuItem(stringResource(label), run { actions.onSort(k, actions.sortAscending) }, checked = actions.sortKey == k) }
                FluentMenuDivider()
                FluentMenuItem(stringResource(R.string.sort_asc), run { actions.onSort(actions.sortKey, true) }, checked = actions.sortAscending)
                FluentMenuItem(stringResource(R.string.sort_desc), run { actions.onSort(actions.sortKey, false) }, checked = !actions.sortAscending)
            }
            Page.Group -> {
                SubHeader(R.string.menu_group_by) { page = Page.Main }
                FluentMenuItem(stringResource(R.string.group_none), run { actions.onGroupBy(GroupBy.None) }, checked = actions.groupBy == GroupBy.None)
                FluentMenuItem(stringResource(R.string.col_modified), run { actions.onGroupBy(GroupBy.Date) }, checked = actions.groupBy == GroupBy.Date)
                FluentMenuItem(stringResource(R.string.col_type), run { actions.onGroupBy(GroupBy.Type) }, checked = actions.groupBy == GroupBy.Type)
            }
            Page.Main -> when (kind) {
                ContextKind.EmptyArea -> {
                    FluentMenuItem(stringResource(R.string.cmd_view), { page = Page.View }, chevron = true)
                    FluentMenuItem(stringResource(R.string.menu_sort_by), { page = Page.Sort }, chevron = true)
                    FluentMenuItem(stringResource(R.string.menu_group_by), { page = Page.Group }, chevron = true)
                    FluentMenuItem(stringResource(R.string.refresh), run(actions.onRefresh))
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.cmd_paste), run(actions.onPaste), enabled = actions.canPaste, icon = FluentIcons.Paste)
                    FluentMenuItem(stringResource(R.string.select_all), run(actions.onSelectAll))
                    actions.onOpenTerminal?.let { FluentMenuItem(stringResource(R.string.ctx_terminal), run(it)) }
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.dlg_new_folder), run(actions.onNewFolder), icon = FluentIcons.Add)
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.ctx_properties), run(actions.onProperties))
                }
                ContextKind.Item -> {
                    if (single) FluentMenuItem(stringResource(R.string.ctx_open), run(actions.onOpenSelected), bold = true)
                    if (single && !actions.singleIsFolder) FluentMenuItem(stringResource(R.string.ctx_open_with), run(actions.onOpenWith))
                    FluentMenuItem(stringResource(R.string.ctx_copy_path), run(actions.onCopyPath))
                    FluentMenuItem(stringResource(R.string.cmd_share), run(actions.onShare), icon = FluentIcons.Share, enabled = !actions.singleIsFolder || !single)
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.cmd_cut), run(actions.onCut), icon = FluentIcons.Cut)
                    FluentMenuItem(stringResource(R.string.cmd_copy), run(actions.onCopy), icon = FluentIcons.Copy)
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.cmd_delete), run(actions.onDelete), icon = FluentIcons.Delete)
                    if (single) FluentMenuItem(stringResource(R.string.cmd_rename), run(actions.onRename), icon = FluentIcons.Rename)
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.ctx_properties), run(actions.onProperties))
                }
                ContextKind.More -> {
                    if (hasSel) {
                        if (single && !actions.singleIsFolder) FluentMenuItem(stringResource(R.string.ctx_open_with), run(actions.onOpenWith))
                        FluentMenuItem(stringResource(R.string.ctx_copy_path), run(actions.onCopyPath))
                        FluentMenuDivider()
                    } else {
                        FluentMenuItem(stringResource(R.string.refresh), run(actions.onRefresh))
                        actions.onOpenTerminal?.let { FluentMenuItem(stringResource(R.string.ctx_terminal), run(it)) }
                        FluentMenuItem(stringResource(R.string.dlg_new_folder), run(actions.onNewFolder), icon = FluentIcons.Add)
                        FluentMenuDivider()
                    }
                    FluentMenuItem(stringResource(R.string.select_all), run(actions.onSelectAll), icon = FluentIcons.Check)
                    FluentMenuItem(stringResource(R.string.select_none), run(actions.onSelectNone))
                    FluentMenuItem(stringResource(R.string.select_invert), run(actions.onInvertSelection))
                    FluentMenuDivider()
                    FluentMenuItem(stringResource(R.string.ctx_properties), run(actions.onProperties))
                    FluentMenuItem(stringResource(R.string.ctx_options), run(actions.onOptions), icon = FluentIcons.Settings)
                }
            }
        }
    }
}

@Composable
private fun SubHeader(title: Int, back: () -> Unit) {
    FluentMenuItem(stringResource(title), back, icon = FluentIcons.Back, bold = true)
    FluentMenuDivider()
}
