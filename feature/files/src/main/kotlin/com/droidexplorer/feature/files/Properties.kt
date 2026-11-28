package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.api.StorageProvider
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ExplorerButton
import com.droidexplorer.ui.design.controls.ExplorerDialog
import com.droidexplorer.ui.design.controls.FluentMenuDivider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PropertiesInfo(
    val name: String,
    val path: String,
    val location: String,
    val single: Boolean,
    val isFolder: Boolean,
    val kind: ItemKind,
    val count: Int,
    val files: Int,
    val folders: Int,
    val bytes: Long,
    val modified: Long?,
    val readOnly: Boolean,
    val hidden: Boolean,
    val permissions: String?,
    val dimensions: String?,
)

/** Reads the facts for the Properties window. Blocking (walks folders); call off the main thread. */
fun computeProperties(provider: StorageProvider, paths: List<String>, fallbackFolder: String): PropertiesInfo {
    val targets = paths.ifEmpty { listOf(fallbackFolder) }
    var files = 0; var folders = 0; var bytes = 0L
    fun walk(path: String) {
        val kids = try { provider.list(path, true) } catch (_: Exception) { emptyList() }
        for (k in kids) if (k.isDirectory) { folders++; walk(k.path) } else { files++; bytes += k.sizeBytes }
    }
    val infos = targets.map { p ->
        val f = File(p)
        StorageItem(p, f.name.ifEmpty { p }, f.isDirectory, f.length(), f.lastModified(), f.name.startsWith("."))
    }
    infos.forEach { if (it.isDirectory) walk(it.path) else { files++; bytes += it.sizeBytes } }
    val first = infos.first()
    val f = File(first.path)
    val single = infos.size == 1
    val perms = if (single) try {
        java.nio.file.Files.getPosixFilePermissions(f.toPath()).let { set ->
            val order = java.nio.file.attribute.PosixFilePermission.values()
            order.joinToString("") { if (it in set) "rwx"[it.ordinal % 3].toString() else "-" }
        }
    } catch (_: Exception) { null } else null
    val dims = if (single && !first.isDirectory && ItemMapping.kindOf(first) == ItemKind.Image) try {
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(first.path, o)
        if (o.outWidth > 0) "${o.outWidth} × ${o.outHeight}" else null
    } catch (_: Exception) { null } else null
    return PropertiesInfo(
        name = first.name,
        path = first.path,
        location = f.parentFile?.path ?: "/",
        single = single,
        isFolder = first.isDirectory,
        kind = ItemMapping.kindOf(first),
        count = infos.size,
        files = files,
        folders = folders,
        bytes = bytes,
        modified = if (single) first.modifiedMillis else null,
        readOnly = single && !f.canWrite(),
        hidden = single && first.isHidden,
        permissions = perms,
        dimensions = dims,
    )
}

/** Windows-style Properties window: General tab, and a Details tab for a single item. */
@Composable
fun PropertiesDialog(provider: StorageProvider, paths: List<String>, currentFolder: String, onClose: () -> Unit) {
    val info by produceState<PropertiesInfo?>(null, paths, currentFolder) {
        value = withContext(Dispatchers.IO) { computeProperties(provider, paths, currentFolder) }
    }
    var tab by remember { mutableStateOf(0) }
    val c = FluentTheme.colors
    val shown = info
    ExplorerDialog(
        title = stringResource(R.string.props_title, shown?.let { if (it.single) it.name else stringResource(R.string.props_multi, it.count) } ?: ""),
        onDismissRequest = onClose,
        buttons = { ExplorerButton(stringResource(R.string.dlg_ok), onClose, default = true) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("props-tabs")) {
            TabLabel(stringResource(R.string.props_general), tab == 0) { tab = 0 }
            if (shown?.single == true) TabLabel(stringResource(R.string.props_details), tab == 1) { tab = 1 }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        if (shown == null) {
            Text(stringResource(R.string.props_calc), style = FluentTheme.typography.body, color = c.textSecondary)
        } else if (tab == 0 || !shown.single) General(shown) else Details(shown)
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = FluentTheme.colors
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = if (selected) FluentTheme.typography.bodyStrong else FluentTheme.typography.body, color = c.textPrimary)
        Box(Modifier.padding(top = 4.dp).width(24.dp).height(2.dp).background(if (selected) c.accent else androidx.compose.ui.graphics.Color.Transparent))
    }
}

@Composable
private fun Line(label: String, value: String) {
    val c = FluentTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = FluentTheme.typography.body, color = c.textSecondary, modifier = Modifier.width(110.dp))
        Text(value, style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content), color = c.textPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun sizeText(p: PropertiesInfo) =
    stringResource(R.string.props_bytes, formatBytes(p.bytes), "%,d".format(p.bytes))

@Composable
private fun General(p: PropertiesInfo) {
    val c = FluentTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (p.isFolder) FolderGlyph(glyphSize = 40.dp) else FileGlyph(p.kind, glyphSize = 40.dp)
        Text(
            if (p.single) p.name else stringResource(R.string.props_multi, p.count),
            style = FluentTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
    if (p.single) Line(stringResource(R.string.props_type), if (p.isFolder) stringResource(R.string.type_folder) else p.kind.typeLabel())
    Line(stringResource(R.string.props_location), p.location)
    Line(stringResource(R.string.props_size), sizeText(p))
    if (p.isFolder || !p.single) Line(stringResource(R.string.props_contains), stringResource(R.string.props_contains_val, p.files, p.folders))
    if (p.single) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        p.modified?.let { Line(stringResource(R.string.props_modified), ItemMapping.format(it)) }
        val attrs = listOfNotNull(
            if (p.readOnly) stringResource(R.string.props_readonly) else null,
            if (p.hidden) stringResource(R.string.props_hidden) else null,
        ).ifEmpty { listOf(stringResource(R.string.props_none)) }.joinToString(", ")
        Line(stringResource(R.string.props_attrs), attrs)
    }
}

@Composable
private fun Details(p: PropertiesInfo) {
    Line(stringResource(R.string.col_name), p.name)
    Line(stringResource(R.string.props_type), if (p.isFolder) stringResource(R.string.type_folder) else p.kind.typeLabel())
    Line(stringResource(R.string.props_path), p.path)
    if (!p.isFolder) Line(stringResource(R.string.props_size), sizeText(p))
    p.modified?.let { Line(stringResource(R.string.props_modified), ItemMapping.format(it)) }
    p.dimensions?.let { Line(stringResource(R.string.props_dims), it) }
    p.permissions?.let { Line(stringResource(R.string.props_perms), it) }
}
