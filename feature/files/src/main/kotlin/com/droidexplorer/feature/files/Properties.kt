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
import com.droidexplorer.ui.design.controls.ExplorerDialogButton
import com.droidexplorer.ui.design.controls.ExplorerDialog
import com.droidexplorer.ui.design.controls.ExplorerTabs
import com.droidexplorer.ui.design.controls.FluentCheckBox
import com.droidexplorer.ui.design.controls.FluentTextBox
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
    /** Space used on disk (from the file system's block count), or null if it cannot be read. */
    val sizeOnDisk: Long?,
    val modified: Long?,
    val readOnly: Boolean,
    val hidden: Boolean,
    val mode: Int?,
    val uid: Int?,
    val gid: Int?,
    val dimensions: String?,
)

private fun statOf(path: String): android.system.StructStat? = try { android.system.Os.stat(path) } catch (_: Throwable) { null }

/** Reads the facts for the Properties window. Blocking (walks folders); call off the main thread. */
fun computeProperties(provider: StorageProvider, paths: List<String>, fallbackFolder: String): PropertiesInfo {
    val targets = paths.ifEmpty { listOf(fallbackFolder) }
    var files = 0; var folders = 0; var bytes = 0L
    var disk = 0L; var diskKnown = true
    fun addDisk(p: String) {
        val st = statOf(p)
        if (st == null) diskKnown = false else disk += st.st_blocks * 512L
    }
    fun walk(path: String) {
        val kids = try { provider.list(path, true) } catch (_: Exception) { emptyList() }
        for (k in kids) {
            addDisk(k.path)
            if (k.isDirectory) { folders++; walk(k.path) } else { files++; bytes += k.sizeBytes }
        }
    }
    val infos = targets.map { p ->
        val f = File(p)
        StorageItem(p, f.name.ifEmpty { p }, f.isDirectory, f.length(), f.lastModified(), f.name.startsWith("."))
    }
    infos.forEach {
        addDisk(it.path)
        if (it.isDirectory) walk(it.path) else { files++; bytes += it.sizeBytes }
    }
    val first = infos.first()
    val f = File(first.path)
    val single = infos.size == 1
    val st = if (single) statOf(first.path) else null
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
        sizeOnDisk = if (diskKnown) disk else null,
        modified = if (single) first.modifiedMillis else null,
        readOnly = single && !f.canWrite(),
        hidden = single && first.isHidden,
        mode = st?.st_mode,
        uid = st?.st_uid,
        gid = st?.st_gid,
        dimensions = dims,
    )
}

/**
 * The Windows Properties window: General, Security (Unix owner and permissions, the Android equivalent) and
 * Details tabs, with an editable name and the OK, Cancel and Apply buttons. Apply renames and sets Read-only.
 * Sharing, Previous Versions and Customize tabs do not exist on Android (see docs/windows-explorer-dialogs.md).
 */
@Composable
fun PropertiesDialog(
    provider: StorageProvider,
    paths: List<String>,
    currentFolder: String,
    onApply: (path: String, newName: String?, readOnly: Boolean?) -> Unit,
    onClose: () -> Unit,
) {
    var info by remember { mutableStateOf<PropertiesInfo?>(null) }
    androidx.compose.runtime.LaunchedEffect(paths, currentFolder) {
        info = withContext(Dispatchers.IO) { computeProperties(provider, paths, currentFolder) }
    }
    var tab by remember { mutableStateOf(0) }
    val c = FluentTheme.colors
    val shown = info
    var name by remember(shown?.path) { mutableStateOf(shown?.name.orEmpty()) }
    var readOnly by remember(shown?.path) { mutableStateOf(shown?.readOnly ?: false) }
    val dirty = shown != null && shown.single && (name.trim() != shown.name || readOnly != shown.readOnly)
    fun apply() {
        val s = shown ?: return
        if (!s.single) return
        onApply(s.path, name.trim().takeIf { it.isNotEmpty() && it != s.name }, readOnly.takeIf { it != s.readOnly && !s.isFolder })
    }
    ExplorerDialog(
        title = stringResource(R.string.props_title, shown?.let { if (it.single) it.name else stringResource(R.string.props_multi, it.count) } ?: ""),
        onDismissRequest = onClose,
        buttons = {
            ExplorerDialogButton(stringResource(R.string.dlg_ok), { apply(); onClose() }, isDefault = true)
            ExplorerDialogButton(stringResource(R.string.dlg_cancel), onClose)
            ExplorerDialogButton(stringResource(R.string.props_apply), { apply() }, enabled = dirty)
        },
    ) {
        if (shown == null) {
            Text(stringResource(R.string.props_calc), style = FluentTheme.typography.body, color = c.textSecondary)
        } else {
            val labels = if (shown.single) listOf(
                stringResource(R.string.props_general), stringResource(R.string.props_security), stringResource(R.string.props_details),
            ) else listOf(stringResource(R.string.props_general))
            ExplorerTabs(labels, tab.coerceAtMost(labels.lastIndex), { tab = it }, Modifier.testTag("props-tabs")) {
                when {
                    tab == 0 || !shown.single -> General(shown, name, { name = it }, readOnly, { readOnly = it })
                    tab == 1 -> Security(shown)
                    else -> Details(shown)
                }
            }
        }
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
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(FluentTheme.colors.divider))
}

@Composable
private fun sizeText(bytes: Long) = stringResource(R.string.props_bytes, formatBytes(bytes), "%,d".format(bytes))

@Composable
private fun General(p: PropertiesInfo, name: String, onName: (String) -> Unit, readOnly: Boolean, onReadOnly: (Boolean) -> Unit) {
    val c = FluentTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (p.isFolder) FolderGlyph(glyphSize = 40.dp) else FileGlyph(p.kind, glyphSize = 40.dp)
        if (p.single) {
            FluentTextBox(value = name, onValueChange = onName, modifier = Modifier.weight(1f).testTag("props-name"))
        } else {
            Text(stringResource(R.string.props_multi, p.count), style = FluentTheme.typography.bodyLarge, color = c.textPrimary)
        }
    }
    Divider()
    if (p.single) Line(stringResource(R.string.props_type), if (p.isFolder) stringResource(R.string.type_folder) else p.kind.typeLabel())
    Line(stringResource(R.string.props_location), p.location)
    Line(stringResource(R.string.props_size), sizeText(p.bytes))
    p.sizeOnDisk?.let { Line(stringResource(R.string.props_size_disk), sizeText(it)) }
    if (p.isFolder || !p.single) Line(stringResource(R.string.props_contains), stringResource(R.string.props_contains_val, p.files, p.folders))
    if (p.single) {
        Divider()
        p.modified?.let { Line(stringResource(R.string.props_modified), ItemMapping.format(it)) }
        Divider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.props_attrs), style = FluentTheme.typography.body, color = c.textSecondary, modifier = Modifier.width(110.dp))
            FluentCheckBox(checked = readOnly, onCheckedChange = onReadOnly, enabled = !p.isFolder, label = stringResource(R.string.props_readonly))
            FluentCheckBox(checked = p.hidden, onCheckedChange = {}, enabled = false, label = stringResource(R.string.props_hidden))
        }
    }
}

/** Owner, group and the read/write/execute rights, the Android counterpart of the Windows Security tab. */
@Composable
private fun Security(p: PropertiesInfo) {
    val c = FluentTheme.colors
    Line(stringResource(R.string.props_object), p.path)
    Divider()
    p.uid?.let { Line(stringResource(R.string.props_owner), it.toString()) }
    p.gid?.let { Line(stringResource(R.string.props_group), it.toString()) }
    val mode = p.mode
    if (mode == null) {
        Text(stringResource(R.string.props_no_perms), style = FluentTheme.typography.body, color = c.textSecondary)
        return
    }
    Divider()
    Row(Modifier.fillMaxWidth()) {
        Text("", Modifier.weight(1.4f))
        listOf(R.string.perm_read, R.string.perm_write, R.string.perm_exec).forEach {
            Text(stringResource(it), style = FluentTheme.typography.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
        }
    }
    listOf(R.string.perm_owner to 6, R.string.perm_group to 3, R.string.perm_others to 0).forEach { (label, shift) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(label), style = FluentTheme.typography.body, color = c.textPrimary, modifier = Modifier.weight(1.4f))
            listOf(4, 2, 1).forEach { bit ->
                val on = (mode shr shift) and bit != 0
                Text(if (on) "✓" else "–", style = FluentTheme.typography.body, color = if (on) c.textPrimary else c.textDisabled, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Details(p: PropertiesInfo) {
    Line(stringResource(R.string.col_name), p.name)
    Line(stringResource(R.string.props_type), if (p.isFolder) stringResource(R.string.type_folder) else p.kind.typeLabel())
    Line(stringResource(R.string.props_path), p.path)
    if (!p.isFolder) Line(stringResource(R.string.props_size), sizeText(p.bytes))
    p.modified?.let { Line(stringResource(R.string.props_modified), ItemMapping.format(it)) }
    p.dimensions?.let { Line(stringResource(R.string.props_dims), it) }
}
