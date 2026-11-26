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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.droidexplorer.storage.api.Conflict
import com.droidexplorer.storage.api.ConflictChoice
import com.droidexplorer.storage.api.ConflictDecision
import com.droidexplorer.storage.api.SortOrder
import com.droidexplorer.storage.api.TransferListener
import com.droidexplorer.storage.api.TransferProgress
import com.droidexplorer.ui.design.controls.FluentCheckBox
import com.droidexplorer.ui.design.controls.FluentProgressBar
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import com.droidexplorer.storage.api.StorageProvider
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.FluentContentDialog
import com.droidexplorer.ui.design.controls.FluentTextBox
import com.droidexplorer.ui.design.icons.FluentIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Dialog {
    data object NewFolder : Dialog
    data object Rename : Dialog
    data object Delete : Dialog
    data class Error(val message: String) : Dialog
}

/** A running copy/move as shown in the progress dialog. */
private data class TransferUi(val progress: TransferProgress, val moving: Boolean, val from: String, val to: String)

/** A conflict waiting for the user; the transfer thread blocks on [answer]. */
private data class PendingConflict(val conflict: Conflict, val answer: CompletableFuture<ConflictDecision?>)

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

    LaunchedEffect(controller.currentPath, controller.refreshTick, controller.query, controller.showHidden) {
        if (controller.query.isNotBlank()) delay(300) // let the person finish typing before walking the tree
        withContext(Dispatchers.IO) { controller.load { isActive } }
    }
    BackHandler(enabled = controller.selection.isNotEmpty()) { controller.clearSelection() }
    BackHandler(enabled = controller.selection.isEmpty() && controller.canBack) { controller.goBack() }

    // Grouping boundaries and rows are computed once per listing, not on every recomposition.
    val atDevice = controller.currentPath == THIS_DEVICE
    val searching = controller.query.isNotBlank() && !atDevice
    val baseRows = remember(controller.items, atDevice, searching) {
        val bounds = ItemMapping.GroupBounds(System.currentTimeMillis())
        controller.items.map {
            val row = ItemMapping.toRow(it, bounds, selected = false)
            when {
                atDevice -> row.copy(modified = "")
                searching -> row.copy(location = it.path.removePrefix(controller.currentPath).removeSuffix("/" + it.name).trim('/').ifEmpty { null })
                else -> row
            }
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
    val effectiveGroup = controller.groupBy ?: if (isDownloads && !searching) GroupBy.Date else GroupBy.None
    val searchHint = stringResource(R.string.search_in, crumbs.last())
    val searchEmpty = stringResource(R.string.search_empty)
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

    var transferUi by remember { mutableStateOf<TransferUi?>(null) }
    var pending by remember { mutableStateOf<PendingConflict?>(null) }
    val cancelFlag = remember { AtomicBoolean(false) }

    fun startPaste() {
        if (controller.clipboard.isEmpty() || transferUi != null) return
        val moving = controller.clipboardIsCut
        val count = controller.clipboard.size
        val from = File(controller.clipboard.first()).parentFile?.name.orEmpty()
        val to = File(controller.currentPath).name
        cancelFlag.set(false)
        val startedAt = System.nanoTime()
        val listener = object : TransferListener {
            override fun onProgress(progress: TransferProgress) {
                // Like Windows, show the progress window only when the operation is not instant.
                val slow = System.nanoTime() - startedAt > 700_000_000L
                if (slow || transferUi != null) transferUi = TransferUi(progress, moving, from, to)
            }
            override fun onConflict(conflict: Conflict): ConflictDecision? {
                val future = CompletableFuture<ConflictDecision?>()
                pending = PendingConflict(conflict, future)
                return future.get()
            }
            override fun isCancelled() = cancelFlag.get()
        }
        scope.launch {
            val outcome = withContext(Dispatchers.IO) { controller.pasteWith(listener) }
            transferUi = null
            pending = null
            val result = outcome.getOrNull()
            if (result != null && result.created.isNotEmpty()) controller.selectAndReveal(result.created)
            else controller.clearSelection()
            controller.refresh()
            outcome.exceptionOrNull()?.let { dialog = Dialog.Error(it.message ?: "error") }
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
        onPaste = ::startPaste,
        onRename = { dialog = Dialog.Rename },
        onShare = { onShare(controller.selection.toList()) },
        onDelete = { dialog = Dialog.Delete },
        onSort = { key, asc -> controller.changeSort(SortOrder(key, asc)) },
        viewMode = controller.viewMode,
        onViewMode = controller::setViewMode,
        groupBy = effectiveGroup,
        onGroupBy = controller::setGroupBy,
        showHidden = controller.showHidden,
        onShowHidden = controller::changeShowHidden,
        onSelectAll = controller::selectAll,
        onSelectNone = controller::clearSelection,
        onInvertSelection = controller::invertSelection,
        query = controller.query,
        onQuery = controller::setQuery,
        searchHint = searchHint,
    )
    ExplorerScreen(
        items = rows,
        actions = actions,
        crumbs = crumbs,
        title = crumbs.last(),
        revealPath = controller.revealPath,
        onRevealed = controller::revealed,
        nav = NavPaneModel(
            quick = quick,
            volumes = controller.volumes,
            currentPath = controller.currentPath,
            volumeFor = controller::volumeFor,
            onNavigate = controller::navigate,
        ),
        emptyText = controller.error?.let { stringResource(R.string.folder_error) }
            ?: if (searching) searchEmpty else stringResource(R.string.folder_empty),
    )

    transferUi?.let { ui ->
        val p = ui.progress
        val percent = if (p.totalBytes > 0) ((p.doneBytes * 100) / p.totalBytes).toInt().coerceIn(0, 100)
        else if (p.totalItems > 0) (p.doneItems * 100 / p.totalItems).coerceIn(0, 100) else 0
        FluentContentDialog(
            title = stringResource(R.string.transfer_title, percent),
            onDismissRequest = {},
            primaryText = stringResource(R.string.dlg_cancel),
            onPrimary = { cancelFlag.set(true) },
        ) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                val text = FluentTheme.typography.body
                val color = FluentTheme.colors.textPrimary
                val line = if (ui.moving) R.string.transfer_moving else R.string.transfer_copying
                Text(stringResource(line, p.totalItems, ui.from, ui.to), style = text, color = color)
                FluentProgressBar(progress = percent / 100f, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                Text(stringResource(R.string.transfer_name, p.currentName), style = text, color = color, maxLines = 1)
                Text(
                    stringResource(R.string.transfer_remaining, (p.totalItems - p.doneItems).coerceAtLeast(0), formatBytes((p.totalBytes - p.doneBytes).coerceAtLeast(0))),
                    style = text, color = color,
                )
                Text(stringResource(R.string.transfer_speed, formatBytes(p.bytesPerSecond)), style = text, color = color)
            }
        }
    }
    pending?.let { pc ->
        var all by remember(pc) { mutableStateOf(false) }
        fun answer(choice: ConflictChoice?) {
            pending = null
            pc.answer.complete(choice?.let { ConflictDecision(it, all) })
        }
        val c = pc.conflict
        FluentContentDialog(
            title = stringResource(R.string.conflict_title),
            onDismissRequest = { answer(null) },
            primaryText = stringResource(R.string.conflict_replace),
            onPrimary = { answer(ConflictChoice.REPLACE) },
            secondaryText = stringResource(R.string.conflict_skip),
            onSecondary = { answer(ConflictChoice.SKIP) },
            tertiaryText = stringResource(R.string.conflict_keep),
            onTertiary = { answer(ConflictChoice.KEEP_BOTH) },
        ) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                val text = FluentTheme.typography.body
                Text(stringResource(R.string.conflict_body, c.source.name), style = text, color = FluentTheme.colors.textPrimary)
                Text(
                    stringResource(R.string.conflict_source, formatBytes(c.source.sizeBytes), ItemMapping.format(c.source.modifiedMillis)),
                    style = text.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Content), color = FluentTheme.colors.textSecondary,
                )
                Text(
                    stringResource(R.string.conflict_existing, formatBytes(c.existing.sizeBytes), ItemMapping.format(c.existing.modifiedMillis)),
                    style = text.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Content), color = FluentTheme.colors.textSecondary,
                )
                FluentCheckBox(checked = all, onCheckedChange = { all = it }, label = stringResource(R.string.conflict_all))
            }
        }
    }

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
