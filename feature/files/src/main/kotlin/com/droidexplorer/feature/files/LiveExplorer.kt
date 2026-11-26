package com.droidexplorer.feature.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.droidexplorer.storage.api.SortOrder
import com.droidexplorer.storage.api.StorageProvider
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.FluentContentDialog
import com.droidexplorer.ui.design.controls.FluentTextBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Dialog {
    data object NewFolder : Dialog
    data object Rename : Dialog
    data object Delete : Dialog
    data class Error(val message: String) : Dialog
}

/**
 * The Explorer connected to a real storage provider, starting at [rootPath].
 * [onOpenFile] and [onShare] are supplied by the app because they need Android intents.
 */
@Composable
fun LiveExplorerScreen(
    provider: StorageProvider,
    rootPath: String,
    onOpenFile: (String) -> Unit = {},
    onShare: (List<String>) -> Unit = {},
) {
    val controller = remember(provider, rootPath) { ExplorerController(provider, rootPath) }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    LaunchedEffect(controller.currentPath, controller.refreshTick) {
        withContext(Dispatchers.IO) { controller.load() }
    }
    BackHandler(enabled = controller.selection.isNotEmpty()) { controller.clearSelection() }
    BackHandler(enabled = controller.selection.isEmpty() && controller.canBack) { controller.goBack() }

    // Grouping boundaries and rows are computed once per listing, not on every recomposition.
    val baseRows = remember(controller.items) {
        val bounds = ItemMapping.GroupBounds(System.currentTimeMillis())
        controller.items.map { ItemMapping.toRow(it, bounds, selected = false) }
    }
    val selection = controller.selection
    val rows = remember(baseRows, selection) {
        if (selection.isEmpty()) baseRows else baseRows.map { if (it.path in selection) it.copy(selected = true) else it }
    }
    val internal = stringResource(R.string.addr_internal)
    val device = stringResource(R.string.addr_this_device)
    val paths = remember(controller.currentPath) { controller.crumbPaths() }
    val crumbs = remember(paths, internal, device) {
        listOf(device) + paths.mapIndexed { i, p -> if (i == 0) internal else p.substringAfterLast('/') }
    }
    val isDownloads = controller.currentPath.substringAfterLast('/') == "Download"
    val newFolderName = stringResource(R.string.new_folder_name)

    fun runOp(op: () -> String?) {
        scope.launch {
            val error = withContext(Dispatchers.IO) { op() }
            controller.clearSelection()
            controller.refresh()
            if (error != null) dialog = Dialog.Error(error)
        }
    }

    val actions = ExplorerActions(
        canBack = controller.canBack,
        canForward = controller.canForward,
        canUp = controller.canUp,
        onBack = controller::goBack,
        onForward = controller::goForward,
        onUp = controller::goUp,
        onRefresh = controller::refresh,
        onOpen = { row ->
            when {
                controller.selection.isNotEmpty() -> controller.toggleSelect(row.path)
                row.kind == ItemKind.Folder -> controller.navigate(row.path)
                else -> onOpenFile(row.path)
            }
        },
        onToggleSelect = { controller.toggleSelect(it.path) },
        // index 0 is the "This device" crumb, which is not a folder.
        onCrumb = { i -> if (i >= 1) controller.navigate(paths[i - 1]) },
        canPaste = controller.clipboard.isNotEmpty(),
        sortKey = controller.sort.key,
        sortAscending = controller.sort.ascending,
        onNewFolder = { dialog = Dialog.NewFolder },
        onCut = controller::cutSelection,
        onCopy = controller::copySelection,
        onPaste = { runOp(controller::paste) },
        onRename = { dialog = Dialog.Rename },
        onShare = { onShare(controller.selection.toList()) },
        onDelete = { dialog = Dialog.Delete },
        onSort = { key, asc -> controller.changeSort(SortOrder(key, asc)) },
    )
    ExplorerScreen(
        items = rows,
        actions = actions,
        crumbs = crumbs,
        title = crumbs.last(),
        grouped = isDownloads,
        emptyText = controller.error?.let { stringResource(R.string.folder_error) }
            ?: stringResource(R.string.folder_empty),
    )

    when (val d = dialog) {
        Dialog.NewFolder -> NameDialog(
            title = stringResource(R.string.dlg_new_folder),
            initial = controller.uniqueName(newFolderName),
            confirmText = stringResource(R.string.dlg_create),
            onDismiss = { dialog = null },
            onConfirm = { name -> dialog = null; runOp { controller.createFolder(name) } },
        )
        Dialog.Rename -> {
            val current = controller.items.firstOrNull { it.path in controller.selection }?.name.orEmpty()
            NameDialog(
                title = stringResource(R.string.cmd_rename),
                initial = current,
                confirmText = stringResource(R.string.cmd_rename),
                onDismiss = { dialog = null },
                onConfirm = { name -> dialog = null; runOp { controller.renameSelected(name) } },
            )
        }
        Dialog.Delete -> FluentContentDialog(
            title = stringResource(R.string.dlg_delete_title),
            onDismissRequest = { dialog = null },
            primaryText = stringResource(R.string.cmd_delete),
            onPrimary = { dialog = null; runOp(controller::deleteSelected) },
            secondaryText = stringResource(R.string.dlg_cancel),
        ) {
            Text(
                stringResource(R.string.dlg_delete_body, controller.selection.size),
                style = FluentTheme.typography.body,
                color = FluentTheme.colors.textPrimary,
            )
        }
        is Dialog.Error -> FluentContentDialog(
            title = stringResource(R.string.dlg_error_title),
            onDismissRequest = { dialog = null },
            primaryText = stringResource(R.string.dlg_ok),
            onPrimary = { dialog = null },
        ) {
            Text(d.message, style = FluentTheme.typography.body, color = FluentTheme.colors.textPrimary)
        }
        null -> Unit
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    FluentContentDialog(
        title = title,
        onDismissRequest = onDismiss,
        primaryText = confirmText,
        onPrimary = { if (text.isNotBlank()) onConfirm(text.trim()) },
        secondaryText = stringResource(R.string.dlg_cancel),
    ) {
        Column {
            FluentTextBox(value = text, onValueChange = { text = it }, modifier = Modifier)
        }
    }
}
