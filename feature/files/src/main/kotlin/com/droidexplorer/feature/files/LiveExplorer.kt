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
import com.droidexplorer.storage.api.NameRules
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
    data object Delete : Dialog
    data object Properties : Dialog
    /** File operation problems: shown in the classic File Explorer style. */
    data class Error(val message: String) : Dialog
    /** Address bar problems: shown in the Windows 11 Settings style. */
    data class PathError(val message: String) : Dialog
    data class ConfirmExtension(val path: String, val newName: String) : Dialog
}

/** A running copy/move as shown in the progress dialog. */
data class TransferUi(
    val progress: TransferProgress,
    val moving: Boolean,
    val from: String,
    val to: String,
    /** Recent speeds (bytes/s), newest last, for the speed graph. */
    val speeds: List<Long> = emptyList(),
)

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
    onOpenWith: (String) -> Unit = {},
    /** Null when no terminal app is installed. */
    onOpenTerminal: ((String) -> Unit)? = null,
) {
    val controller = remember(provider, rootPath, volumes) { ExplorerController(provider, rootPath, volumes = volumes) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val tree = remember(provider) { NavTreeState(provider) }
    LaunchedEffect(controller.refreshTick) { tree.invalidate() }
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
    val pauseFlag = remember { AtomicBoolean(false) }
    var paused by remember { mutableStateOf(false) }

    fun startPaste() {
        if (controller.clipboard.isEmpty() || transferUi != null) return
        val moving = controller.clipboardIsCut
        val count = controller.clipboard.size
        val from = File(controller.clipboard.first()).parentFile?.name.orEmpty()
        val to = File(controller.currentPath).name
        cancelFlag.set(false)
        pauseFlag.set(false)
        paused = false
        val startedAt = System.nanoTime()
        val listener = object : TransferListener {
            override fun onProgress(progress: TransferProgress) {
                // Like Windows, show the progress window only when the operation is not instant.
                val slow = System.nanoTime() - startedAt > 700_000_000L
                if (slow || transferUi != null) {
                    val old = transferUi?.speeds.orEmpty()
                    transferUi = TransferUi(progress, moving, from, to, (old + progress.bytesPerSecond).takeLast(60))
                }
            }
            override fun onConflict(conflict: Conflict): ConflictDecision? {
                val future = CompletableFuture<ConflictDecision?>()
                pending = PendingConflict(conflict, future)
                return future.get()
            }
            override fun isCancelled() = cancelFlag.get()
            override fun isPaused() = pauseFlag.get()
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

    val errEmpty = stringResource(R.string.rename_err_empty)
    val errInvalid = stringResource(R.string.rename_err_invalid)
    val errExists = stringResource(R.string.rename_err_exists)

    fun doRename(path: String, name: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { provider.rename(path, name) } }
            controller.cancelRename()
            result.onSuccess { controller.refresh(); controller.selectAndReveal(listOf(it.path)) }
            result.exceptionOrNull()?.let { e ->
                dialog = Dialog.Error(if (e.message.orEmpty().startsWith("Already exists")) errExists else e.message ?: errInvalid)
            }
        }
    }

    fun commitRename(path: String, typed: String) {
        val old = File(path).name
        val name = typed.trim()
        when {
            name == old -> controller.cancelRename()
            name.isEmpty() -> { controller.cancelRename(); dialog = Dialog.Error(errEmpty) }
            NameRules.validate(name) != null -> { controller.cancelRename(); dialog = Dialog.Error(errInvalid) }
            // Windows warns before a file's extension is changed or removed.
            !File(path).isDirectory && old.substringAfterLast('.', "") != name.substringAfterLast('.', "") &&
                old.contains('.') -> dialog = Dialog.ConfirmExtension(path, name)
            else -> doRename(path, name)
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
                if (ok) controller.navigate(target) else dialog = Dialog.PathError(notFound)
            }
        },
        canPaste = controller.clipboard.isNotEmpty(),
        sortKey = controller.sort.key,
        sortAscending = controller.sort.ascending,
        onNewFolder = {
            // Like Windows: the folder appears at once with its name selected for typing, no dialog.
            scope.launch {
                val name = controller.uniqueName(newFolderName)
                val created = withContext(Dispatchers.IO) { runCatching { provider.createFolder(controller.currentPath, name) } }
                created.onSuccess {
                    controller.refresh()
                    controller.selectAndReveal(listOf(it.path))
                    controller.startRename(it.path)
                }
                created.exceptionOrNull()?.let { e -> dialog = Dialog.Error(e.message ?: "error") }
            }
        },
        // Leaving selection mode after Cut/Copy lets a tap open folders again, so the person can go and paste.
        onCut = { controller.cutSelection(); controller.clearSelection() },
        onCopy = { controller.copySelection(); controller.clearSelection() },
        onPaste = ::startPaste,
        onRename = { controller.selection.singleOrNull()?.let(controller::startRename) },
        onShare = { onShare(controller.selection.toList()) },
        onOpenSelected = {
            controller.selection.singleOrNull()?.let { p ->
                val item = controller.items.firstOrNull { it.path == p }
                if (item?.isDirectory == true) controller.navigate(p) else onOpenFile(p)
            }
        },
        onOpenWith = { controller.selection.singleOrNull()?.let(onOpenWith) },
        onCopyPath = {
            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = controller.selection.ifEmpty { setOf(controller.currentPath) }.joinToString("\n") { "\"" + it + "\"" }
            cm.setPrimaryClip(android.content.ClipData.newPlainText("path", text))
        },
        onProperties = { dialog = Dialog.Properties },
        onOpenTerminal = onOpenTerminal?.let { open -> { open(controller.currentPath) } },
        onDelete = { dialog = Dialog.Delete },
        onSort = { key, asc -> controller.changeSort(SortOrder(key, asc)) },
        viewMode = controller.viewMode,
        onViewMode = controller::chooseViewMode,
        groupBy = effectiveGroup,
        onGroupBy = controller::chooseGroupBy,
        showHidden = controller.showHidden,
        onShowHidden = controller::changeShowHidden,
        onSelectAll = controller::selectAll,
        onSelectNone = controller::clearSelection,
        onInvertSelection = controller::invertSelection,
        query = controller.query,
        onQuery = controller::updateQuery,
        searchHint = searchHint,
        renamingPath = controller.renamingPath,
        onRenameCommit = ::commitRename,
        onRenameCancel = controller::cancelRename,
    )
    ExplorerScreen(
        items = rows,
        actions = actions,
        crumbs = crumbs,
        title = crumbs.last(),
        revealPath = controller.revealPath,
        onRevealed = controller::revealed,
        nav = NavPaneModel(
            tree = tree,
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
        TransferProgressDialog(
            ui, paused,
            onTogglePause = { paused = !paused; pauseFlag.set(paused) },
            onCancel = { cancelFlag.set(true); pauseFlag.set(false) },
        )
    }
    pending?.let { pc ->
        ConflictDialogs(pc.conflict, pc.conflict.totalItems, controller.clipboardIsCut) { answer ->
            pending = null
            pc.answer.complete(answer?.let { ConflictDecision(it.choice, it.applyToAll, it.skipIdentical) })
        }
    }

    when (val d = dialog) {
        Dialog.Properties -> PropertiesDialog(provider, controller.selection.toList(), controller.currentPath) { dialog = null }
        Dialog.Delete -> DeleteConfirmDialog(
            items = controller.items.filter { it.path in controller.selection },
            onYes = { dialog = null; runOp(controller::deleteSelected) },
            onNo = { dialog = null },
        )
        is Dialog.Error -> ClassicMessageDialog(stringResource(R.string.dlg_error_title), d.message) { dialog = null }
        is Dialog.PathError -> FluentContentDialog(
            title = stringResource(R.string.dlg_error_title),
            onDismissRequest = { dialog = null },
            primaryText = stringResource(R.string.dlg_ok),
            onPrimary = { dialog = null },
        ) {
            Text(d.message, style = FluentTheme.typography.body, color = FluentTheme.colors.textPrimary)
        }
        is Dialog.ConfirmExtension -> ClassicQuestionDialog(
            title = stringResource(R.string.cmd_rename),
            question = stringResource(R.string.rename_ext_body),
            onYes = { dialog = null; doRename(d.path, d.newName) },
            onNo = { dialog = null; controller.cancelRename() },
        )
        null -> Unit
    }
}
