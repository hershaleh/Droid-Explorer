package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.testTag
import com.droidexplorer.storage.api.SortOrder
import com.droidexplorer.storage.api.Sorting
import com.droidexplorer.storage.api.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun NavTreeItem(
    label: String,
    icon: @Composable () -> Unit,
    depth: Int = 0,
    expanded: Boolean? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onToggle: (() -> Unit)? = null,
    toggleTag: String? = null,
) {
    val c = FluentTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .height(32.dp)
            .clip(FluentShapes.control)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (selected) c.subtleFillSecondary else Color.Transparent, FluentShapes.control),
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(16.dp)
                    .background(c.accent, RoundedCornerShape(2.dp)),
            )
        }
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = (4 + depth * 16).dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 24.dp, height = 32.dp)
                    .then(if (onToggle != null && expanded != null) Modifier.clickable(onClick = onToggle) else Modifier)
                    .then(if (toggleTag != null) Modifier.testTag(toggleTag) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (expanded != null) {
                    FluentIcon(
                        if (expanded) FluentIcons.ChevronDown else FluentIcons.ChevronRight,
                        tint = c.textSecondary,
                        size = 10.dp,
                    )
                }
            }
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = FluentTheme.typography.body,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(1.dp)
            .background(FluentTheme.colors.controlStrokeSecondary),
    )
}

/** A Quick access shortcut shown in the live navigation pane. */
data class QuickEntry(val label: String, val icon: FluentIcons, val path: String)

/**
 * Expanded/collapsed state and lazily loaded sub-folders of the navigation tree.
 * "This device" and "Quick access" start expanded, like in Windows.
 */
class NavTreeState(private val provider: StorageProvider) {
    private val expanded = mutableStateMapOf(THIS_DEVICE to true, QUICK_ACCESS to true)
    private val loaded = mutableStateMapOf<String, List<String>>()

    fun isExpanded(key: String) = expanded[key] == true
    fun expand(key: String) { expanded[key] = true }
    fun toggle(key: String) { expanded[key] = !isExpanded(key) }
    fun childrenOf(path: String): List<String>? = loaded[path]

    /** Forget loaded folders so expanded nodes reload (after a refresh or a file operation). */
    fun invalidate() = loaded.clear()

    suspend fun load(path: String) {
        if (loaded.containsKey(path)) return
        val folders = withContext(Dispatchers.IO) {
            try {
                Sorting.sort(provider.list(path, false).filter { it.isDirectory }, SortOrder()).map { it.path }
            } catch (_: Exception) {
                emptyList()
            }
        }
        loaded[path] = folders
    }

    companion object { const val QUICK_ACCESS = "::quick-access" }
}

/** What the live navigation pane shows and does. Only working destinations are listed. */
data class NavPaneModel(
    val tree: NavTreeState,
    val quick: List<QuickEntry>,
    val volumes: List<VolumeInfo>,
    val currentPath: String,
    val volumeFor: (String) -> VolumeInfo?,
    val onNavigate: (String) -> Unit,
)

@Composable
private fun FolderNode(path: String, label: String, icon: @Composable () -> Unit, depth: Int, model: NavPaneModel) {
    val tree = model.tree
    val open = tree.isExpanded(path)
    val kids = tree.childrenOf(path)
    LaunchedEffect(path, open, kids == null) { if (open) tree.load(path) }
    NavTreeItem(
        label = label,
        icon = icon,
        depth = depth,
        expanded = if (kids != null && kids.isEmpty()) null else open,
        selected = model.currentPath == path,
        onClick = { model.onNavigate(path) },
        onToggle = { tree.toggle(path) },
        toggleTag = "nav-toggle-$path",
    )
    if (open) kids?.forEach { child -> FolderNode(child, child.substringAfterLast('/'), { FolderGlyph() }, depth + 1, model) }
}

@Composable
private fun LiveNavPane(model: NavPaneModel, modifier: Modifier) {
    val c = FluentTheme.colors
    val internal = stringResource(R.string.addr_internal)
    val tree = model.tree
    // Open the tree down to the folder being shown, so it is always visible and highlighted.
    LaunchedEffect(model.currentPath) {
        val root = model.volumeFor(model.currentPath)?.path ?: return@LaunchedEffect
        if (!model.currentPath.startsWith(root)) return@LaunchedEffect
        tree.expand(root)
        var acc = root.trimEnd('/')
        model.currentPath.removePrefix(root).trim('/').split('/').filter { it.isNotEmpty() }.dropLast(1).forEach {
            acc = "$acc/$it"
            tree.expand(acc)
        }
    }
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(c.windowBackground)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        NavTreeItem(
            stringResource(R.string.nav_quick_access),
            { FluentIcon(FluentIcons.Home, tint = c.accent) },
            expanded = tree.isExpanded(NavTreeState.QUICK_ACCESS),
            onToggle = { tree.toggle(NavTreeState.QUICK_ACCESS) },
        )
        if (tree.isExpanded(NavTreeState.QUICK_ACCESS)) {
            model.quick.forEach { q ->
                NavTreeItem(
                    q.label,
                    { FluentIcon(q.icon) },
                    depth = 1,
                    selected = model.currentPath == q.path,
                    onClick = { model.onNavigate(q.path) },
                )
            }
        }
        NavDivider()
        NavTreeItem(
            stringResource(R.string.nav_this_device),
            { FluentIcon(FluentIcons.Network) },
            expanded = tree.isExpanded(THIS_DEVICE),
            selected = model.currentPath == THIS_DEVICE,
            onClick = { model.onNavigate(THIS_DEVICE) },
            onToggle = { tree.toggle(THIS_DEVICE) },
        )
        if (tree.isExpanded(THIS_DEVICE)) {
            model.volumes.forEach { v ->
                FolderNode(v.path, v.label.ifEmpty { internal }, { FluentIcon(FluentIcons.Drive) }, 1, model)
            }
        }
    }
}

/** Navigation pane on the Mica background. With a [model] it is live; without one it is a static preview. */
@Composable
fun ExplorerNavPane(modifier: Modifier = Modifier, model: NavPaneModel? = null) {
    if (model != null) {
        LiveNavPane(model, modifier)
        return
    }
    val c = FluentTheme.colors
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(c.windowBackground)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        NavTreeItem(stringResource(R.string.nav_home), { FluentIcon(FluentIcons.Home) })
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_quick_access), { FluentIcon(FluentIcons.Home, tint = c.accent) }, expanded = true)
        NavTreeItem(stringResource(R.string.nav_downloads), { FluentIcon(FluentIcons.Download) }, depth = 1, selected = true)
        NavTreeItem(stringResource(R.string.nav_documents), { FluentIcon(FluentIcons.Document) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_pictures), { FluentIcon(FluentIcons.Picture) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_music), { FluentIcon(FluentIcons.Music) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_videos), { FluentIcon(FluentIcons.Video) }, depth = 1)
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_this_device), { FluentIcon(FluentIcons.Network) }, expanded = true)
        NavTreeItem(stringResource(R.string.nav_internal), { FluentIcon(FluentIcons.Drive) }, depth = 1, expanded = false)
        NavTreeItem(stringResource(R.string.nav_sd), { FluentIcon(FluentIcons.Drive) }, depth = 1, expanded = false)
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_cloud), { FluentIcon(FluentIcons.Cloud) }, expanded = false)
        NavTreeItem(stringResource(R.string.nav_network), { FluentIcon(FluentIcons.Network) }, expanded = false)
    }
}
