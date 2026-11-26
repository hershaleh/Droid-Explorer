package com.droidexplorer.feature.files

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.droidexplorer.storage.api.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The Explorer connected to a real storage provider, starting at [rootPath]. */
@Composable
fun LiveExplorerScreen(provider: StorageProvider, rootPath: String) {
    val controller = remember(provider, rootPath) { ExplorerController(provider, rootPath) }
    LaunchedEffect(controller.currentPath, controller.refreshTick) {
        withContext(Dispatchers.IO) { controller.load() }
    }
    BackHandler(enabled = controller.canBack) { controller.goBack() }

    val now = System.currentTimeMillis()
    val rows = controller.items.map { ItemMapping.toRow(it, now, it.path in controller.selection) }
    val internal = stringResource(R.string.addr_internal)
    val device = stringResource(R.string.addr_this_device)
    val paths = controller.crumbPaths()
    val crumbs = listOf(device) + paths.mapIndexed { i, p -> if (i == 0) internal else p.substringAfterLast('/') }
    val isDownloads = controller.currentPath.substringAfterLast('/') == "Download"

    val actions = ExplorerActions(
        canBack = controller.canBack,
        canForward = controller.canForward,
        canUp = controller.canUp,
        onBack = controller::goBack,
        onForward = controller::goForward,
        onUp = controller::goUp,
        onRefresh = controller::refresh,
        onOpen = { row -> if (row.kind == ItemKind.Folder) controller.navigate(row.path) },
        onToggleSelect = { controller.toggleSelect(it.path) },
        // index 0 is the "This device" crumb, which is not a folder.
        onCrumb = { i -> if (i >= 1) controller.navigate(paths[i - 1]) },
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
}
