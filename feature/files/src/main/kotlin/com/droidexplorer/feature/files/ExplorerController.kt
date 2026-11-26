package com.droidexplorer.feature.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.droidexplorer.storage.api.SortOrder
import com.droidexplorer.storage.api.Sorting
import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.api.StorageProvider
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

    /** Windows File Explorer date groups, relative to [now]. Weeks start on [firstDayOfWeek]. */
    fun groupOf(modifiedMillis: Long, now: Long, firstDayOfWeek: Int = Calendar.SUNDAY): DateGroup {
        fun startOfDay(t: Long) = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val today = startOfDay(now)
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val weekStart = (today.clone() as Calendar).apply {
            firstDayOfWeek.let { f -> this.firstDayOfWeek = f }
            while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_YEAR, -1)
        }
        val lastWeekStart = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val monthStart = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lastMonthStart = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val yearStart = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1) }
        val m = modifiedMillis
        return when {
            m >= today.timeInMillis -> DateGroup.Today
            m >= yesterday.timeInMillis -> DateGroup.Yesterday
            m >= weekStart.timeInMillis -> DateGroup.EarlierThisWeek
            m >= lastWeekStart.timeInMillis -> DateGroup.LastWeek
            m >= monthStart.timeInMillis -> DateGroup.EarlierThisMonth
            m >= lastMonthStart.timeInMillis -> DateGroup.LastMonth
            m >= yearStart.timeInMillis -> DateGroup.EarlierThisYear
            else -> DateGroup.LongAgo
        }
    }

    fun format(modifiedMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(modifiedMillis))

    fun toRow(item: StorageItem, now: Long, selected: Boolean): ExplorerItem = ExplorerItem(
        name = item.name,
        kind = kindOf(item),
        modified = format(item.modifiedMillis),
        sizeKb = if (item.isDirectory) null else (item.sizeBytes + 1023) / 1024,
        group = groupOf(item.modifiedMillis, now),
        selected = selected,
        path = item.path,
    )
}

/**
 * Navigation state of one Explorer tab: current folder, back/forward history, selection and sort.
 * Listing is done by the caller off the main thread via [load].
 */
class ExplorerController(
    private val provider: StorageProvider,
    val rootPath: String,
    startPath: String = rootPath,
) {
    var currentPath by mutableStateOf(startPath); private set
    var items by mutableStateOf<List<StorageItem>>(emptyList()); private set
    var selection by mutableStateOf<Set<String>>(emptySet()); private set
    var error by mutableStateOf<String?>(null); private set
    var sort by mutableStateOf(SortOrder()); private set
    var refreshTick by mutableStateOf(0); private set

    private val back = ArrayDeque<String>()
    private val forward = ArrayDeque<String>()

    val canBack get() = back.isNotEmpty()
    val canForward get() = forward.isNotEmpty()
    val canUp get() = currentPath != rootPath && parentOf(currentPath) != null

    fun navigate(path: String) {
        if (path == currentPath) return
        back.addLast(currentPath)
        forward.clear()
        go(path)
    }

    fun goBack() { back.removeLastOrNull()?.let { forward.addLast(currentPath); go(it) } }
    fun goForward() { forward.removeLastOrNull()?.let { back.addLast(currentPath); go(it) } }
    fun goUp() { if (canUp) parentOf(currentPath)?.let { navigate(it) } }
    fun refresh() { refreshTick++ }

    fun toggleSelect(path: String) {
        selection = if (path in selection) selection - path else selection + path
    }

    fun clearSelection() { selection = emptySet() }

    /** Breadcrumb paths from [rootPath] to [currentPath]. */
    fun crumbPaths(): List<String> {
        val result = mutableListOf(rootPath)
        val rest = currentPath.removePrefix(rootPath).trim('/')
        if (rest.isNotEmpty()) {
            var acc = rootPath.trimEnd('/')
            rest.split('/').forEach { part -> acc = "$acc/$part"; result += acc }
        }
        return result
    }

    /** Blocking; call from an IO dispatcher. */
    fun load() {
        val path = currentPath
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
