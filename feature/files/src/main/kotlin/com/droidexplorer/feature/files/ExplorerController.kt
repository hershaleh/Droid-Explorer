package com.droidexplorer.feature.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.droidexplorer.storage.api.SortOrder
import com.droidexplorer.storage.api.Sorting
import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.api.StorageProvider
import com.droidexplorer.storage.api.TransferListener
import com.droidexplorer.storage.api.TransferResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pure mapping helpers between storage items and rows (unit-tested without Android). */
object ItemMapping {
    fun kindOf(item: StorageItem): ItemKind {
        if (item.isDirectory) return ItemKind.Folder
        return when (Sorting.extension(item.name)) {
            "pdf" -> ItemKind.Pdf
            "zip", "rar", "7z", "tar", "gz" -> ItemKind.Zip
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> ItemKind.Image
            "mp3", "m4a", "wav", "flac", "ogg", "aac" -> ItemKind.Audio
            "mp4", "mkv", "avi", "mov", "webm", "3gp" -> ItemKind.Video
            "doc", "docx", "odt", "xls", "xlsx", "ppt", "pptx" -> ItemKind.Document
            "apk" -> ItemKind.Apk
            else -> ItemKind.Text
        }
    }

    /** Day/week/month boundaries computed once per listing, so grouping thousands of files stays cheap. */
    class GroupBounds(now: Long, firstDayOfWeek: Int = Calendar.SUNDAY) {
        private fun startOfDay(t: Long) = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        private val today = startOfDay(now)
        private val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        private val weekStart = (today.clone() as Calendar).apply {
            while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_YEAR, -1)
        }
        private val lastWeekStart = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        private val monthStart = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        private val lastMonthStart = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }.timeInMillis
        private val yearStart = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        private val todayMs = today.timeInMillis
        private val weekMs = weekStart.timeInMillis
        private val monthMs = monthStart.timeInMillis

        fun groupOf(m: Long): DateGroup = when {
            m >= todayMs -> DateGroup.Today
            m >= yesterday -> DateGroup.Yesterday
            m >= weekMs -> DateGroup.EarlierThisWeek
            m >= lastWeekStart -> DateGroup.LastWeek
            m >= monthMs -> DateGroup.EarlierThisMonth
            m >= lastMonthStart -> DateGroup.LastMonth
            m >= yearStart -> DateGroup.EarlierThisYear
            else -> DateGroup.LongAgo
        }
    }

    fun groupOf(modifiedMillis: Long, now: Long, firstDayOfWeek: Int = Calendar.SUNDAY): DateGroup =
        GroupBounds(now, firstDayOfWeek).groupOf(modifiedMillis)

    fun format(modifiedMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(modifiedMillis))

    fun toRow(item: StorageItem, now: Long, selected: Boolean): ExplorerItem =
        toRow(item, GroupBounds(now), selected)

    fun toRow(item: StorageItem, bounds: GroupBounds, selected: Boolean): ExplorerItem = ExplorerItem(
        name = item.name,
        kind = kindOf(item),
        modified = format(item.modifiedMillis),
        sizeKb = if (item.isDirectory) null else (item.sizeBytes + 1023) / 1024,
        group = bounds.groupOf(item.modifiedMillis),
        selected = selected,
        path = item.path,
    )
}

/** A storage volume (internal storage, SD card, USB drive) shown under "This device". */
data class VolumeInfo(val label: String, val path: String, val removable: Boolean = false)

/** Virtual location listing all volumes, like "This PC" in Windows. */
const val THIS_DEVICE = "::this-device"

/**
 * Navigation state of one Explorer tab: current folder, back/forward history, selection and sort.
 * Listing is done by the caller off the main thread via [load].
 */
class ExplorerController(
    private val provider: StorageProvider,
    val rootPath: String,
    startPath: String = rootPath,
    val volumes: List<VolumeInfo> = listOf(VolumeInfo("", rootPath)),
) {
    var currentPath by mutableStateOf(startPath); private set
    var items by mutableStateOf<List<StorageItem>>(emptyList()); private set
    var selection by mutableStateOf<Set<String>>(emptySet()); private set
    var error by mutableStateOf<String?>(null); private set
    var sort by mutableStateOf(SortOrder()); private set
    var refreshTick by mutableStateOf(0); private set

    var clipboard by mutableStateOf<List<String>>(emptyList()); private set
    var clipboardIsCut by mutableStateOf(false); private set
    var revealPath by mutableStateOf<String?>(null); private set

    private val back = ArrayDeque<String>()
    private val forward = ArrayDeque<String>()

    val canBack get() = back.isNotEmpty()
    val canForward get() = forward.isNotEmpty()
    val canUp get() = currentPath != THIS_DEVICE && (isVolumeRoot(currentPath) || parentOf(currentPath) != null)

    /** The volume containing [path] (the most specific one), or null for the virtual "This device" page. */
    fun volumeFor(path: String): VolumeInfo? = volumes
        .filter { path == it.path || path.startsWith(it.path.trimEnd('/') + "/") }
        .maxByOrNull { it.path.length }

    private fun isVolumeRoot(path: String) = volumes.any { it.path.trimEnd('/') == path.trimEnd('/') }

    fun navigate(path: String) {
        if (path == currentPath) return
        back.addLast(currentPath)
        forward.clear()
        go(path)
    }

    fun goBack() { back.removeLastOrNull()?.let { forward.addLast(currentPath); go(it) } }
    fun goForward() { forward.removeLastOrNull()?.let { back.addLast(currentPath); go(it) } }
    fun goUp() {
        when {
            !canUp -> Unit
            isVolumeRoot(currentPath) -> navigate(THIS_DEVICE)
            else -> parentOf(currentPath)?.let { navigate(it) }
        }
    }
    fun refresh() { refreshTick++ }

    fun toggleSelect(path: String) {
        selection = if (path in selection) selection - path else selection + path
    }

    fun changeSort(order: SortOrder) {
        sort = order
        items = Sorting.sort(items, order)
    }

    fun copySelection() { clipboard = selection.toList(); clipboardIsCut = false }
    fun cutSelection() { clipboard = selection.toList(); clipboardIsCut = true }

    /** Each operation returns null on success or the error message. Blocking: run off the main thread. */
    fun paste(): String? = attempt { pasteWith(object : TransferListener {}).getOrThrow() }

    /** Pastes the clipboard here with progress and conflict questions reported to [listener]. Blocking. */
    fun pasteWith(listener: TransferListener): Result<TransferResult> = runCatching {
        val result = provider.transfer(clipboard, currentPath, clipboardIsCut, listener)
        if (clipboardIsCut && !result.cancelled) clipboard = emptyList()
        result
    }

    /** Select [paths] and scroll the first one into view once the listing has refreshed. */
    fun selectAndReveal(paths: List<String>) {
        selection = paths.toSet()
        revealPath = paths.firstOrNull()
    }

    fun revealed() { revealPath = null }

    fun createFolder(name: String): String? = attempt { provider.createFolder(currentPath, name) }

    fun renameSelected(newName: String): String? = attempt {
        val path = selection.singleOrNull() ?: error("Select exactly one item")
        provider.rename(path, newName)
    }

    fun deleteSelected(): String? = attempt {
        selection.forEach { provider.delete(it) }
    }

    /** "New folder", then "New folder (2)", "New folder (3)" as Windows does. */
    fun uniqueName(base: String): String {
        val taken = items.map { it.name }.toSet()
        if (base !in taken) return base
        var n = 2
        while ("$base ($n)" in taken) n++
        return "$base ($n)"
    }

    private fun attempt(block: () -> Unit): String? = try {
        block()
        null
    } catch (e: Exception) {
        e.message ?: "error"
    }

    fun clearSelection() { selection = emptySet() }

    /** Breadcrumb paths from the containing volume's root to [currentPath] (empty on the "This device" page). */
    fun crumbPaths(): List<String> {
        if (currentPath == THIS_DEVICE) return emptyList()
        val root = volumeFor(currentPath)?.path ?: rootPath
        val result = mutableListOf(root)
        val rest = currentPath.removePrefix(root).trim('/')
        if (rest.isNotEmpty()) {
            var acc = root.trimEnd('/')
            rest.split('/').forEach { part -> acc = "$acc/$part"; result += acc }
        }
        return result
    }

    /** Blocking; call from an IO dispatcher. */
    fun load() {
        val path = currentPath
        if (path == THIS_DEVICE) {
            items = volumes.map { StorageItem(it.path, it.label.ifEmpty { it.path }, true, 0, 0, false) }
            error = null
            return
        }
        try {
            val listed = Sorting.sort(provider.list(path), sort)
            items = listed
            error = null
        } catch (e: Exception) {
            items = emptyList()
            error = e.message ?: "error"
        }
    }

    private fun go(path: String) {
        currentPath = path
        selection = emptySet()
    }

    private fun parentOf(path: String): String? = path.trimEnd('/').substringBeforeLast('/', "").ifEmpty { null }
}
