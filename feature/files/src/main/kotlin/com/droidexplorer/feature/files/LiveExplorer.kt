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
import com.droidexplorer.ui.design.icons.FluentIcons
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
    volumes: List<VolumeInfo> = listOf(VolumeInfo("", rootPath)),
    onOpenFile: (String) -> Unit = {},
    onShare: (List<String>) -> Unit = {},
) {
    val controller = remember(provider, rootPath, volumes) { ExplorerController(provider, rootPath, volumes = volumes) }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    LaunchedEffect(controller.currentPath, controller.refreshTick) {
        withContext(Dispatchers.IO) { controller.load() }
    }
    BackHandler(enabled = controller.selection.isNotEmpty()) { controller.clearSelection() }
    BackHandler(enabled = controller.selection.isEmpty() && controller.canBack) { controller.goBack() }

    // Grouping boundaries and rows are computed once per listing, not on every recomposition.
    val atDevice = controller.currentPath == THIS_DEVICE
    val baseRows = remember(controller.items, atDevice) {
        val bounds = ItemMapping.GroupBounds(System.currentTimeMillis())
        controller.items.map {
            val row = ItemMapping.toRow(it, bounds, selected = false)
            if (atDevice) row.copy(modified = "") else row
        }
    }
    val selection = controller.selection
    val rows = remember(baseRows, selection) {
        if (selection.isEmpty()) baseRows else baseRows.map { if (it.path in selection) it.copy(selected = true) else it }
    }
    val internal = stringResource(R.string.addr_internal)
    val device = stringResource(R.string.addr_this_device)
    val paths = remember(controller.currentPath) { controller.crumbPaths() }
    val volumeLabel = controller.volumeFor(controller.currentPath)?.label?.ifEmpty { internal } ?: internal
    val crumbs = remember(paths, volumeLabel, device) {
        listOf(device) + paths.mapIndexed { i, p -> if (i == 0) volumeLabel else p.substringAfterLast('/') }
    }
    val isDownloads = !atDevice && controller.currentPath.substringAfterLast('/') == "Download"
    val notFound = stringResource(R.string.path_not_found)
    val quick = listOf(
        QuickEntry(stringResource(R.string.nav_downloads), FluentIcons.Download, "Download"),
        QuickEntry(stringResource(R.string.nav_documents), FluentIcons.Document, "Documents"),
        QuickEntry(stringResource(R.string.nav_pictures), FluentIcons.Picture, "Pictures"),
        QuickEntry(stringResource(R.string.nav_music), FluentIcons.Music, "Music"),
        QuickEntry(stringResource(R.string.nav_videos), FluentIcons.Video, "Movies"),
    ).map { it.copy(path = rootPath.trimEnd('/') + "/" + it.path) }
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
        onCrumb = { i ->
            if (i == 0) controller.navigate(THIS_DEVICE) else paths.getOrNull(i - 1)?.let(controller::navigate)
        },
        editablePath = if (atDevice) null else controller.currentPath,
        onPathSubmit = { text ->
            scope.launch {
                val target = text.trimEnd('/').ifEmpty { "/" }
                val ok = withContext(Dispatchers.IO) { runCatching { provider.list(target) }.isSuccess }
                if (ok) controller.navigate(target) else dialog = Dialog.Error(notFound)
            }
        },
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
        nav = NavPaneModel(
            quick = quick,
            volumes = controller.volumes,
            currentPath = controller.currentPath,
            volumeFor = controller::volumeFor,
            onNavigate = controller::navigate,
        ),
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
