package com.droidexplorer.feature.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import java.util.Locale

enum class ItemKind { Folder, Pdf, Zip, Image, Audio, Video, Document, Apk, Text }

enum class DateGroup { Today, Yesterday, EarlierThisWeek, LastWeek, EarlierThisMonth, LastMonth, EarlierThisYear, LongAgo }

/** One row of the details list. [sizeKb] is null for folders. */
data class ExplorerItem(
    val name: String,
    val kind: ItemKind,
    val modified: String,
    val sizeKb: Long?,
    val group: DateGroup,
    val selected: Boolean = false,
    val path: String = name,
    val location: String? = null,
)

@Composable
fun ItemKind.typeLabel(): String = stringResource(
    when (this) {
        ItemKind.Folder -> R.string.type_folder
        ItemKind.Pdf -> R.string.type_pdf
        ItemKind.Zip -> R.string.type_zip
        ItemKind.Image -> R.string.type_image
        ItemKind.Audio -> R.string.type_audio
        ItemKind.Video -> R.string.type_video
        ItemKind.Document -> R.string.type_document
        ItemKind.Apk -> R.string.type_apk
        ItemKind.Text -> R.string.type_text
    },
)

@Composable
fun DateGroup.label(): String = stringResource(
    when (this) {
        DateGroup.Today -> R.string.group_today
        DateGroup.Yesterday -> R.string.group_yesterday
        DateGroup.EarlierThisWeek -> R.string.group_this_week
        DateGroup.LastWeek -> R.string.group_last_week
        DateGroup.EarlierThisMonth -> R.string.group_this_month
        DateGroup.LastMonth -> R.string.group_last_month
        DateGroup.EarlierThisYear -> R.string.group_this_year
        DateGroup.LongAgo -> R.string.group_long_ago
    },
)

/** Windows Explorer shows file sizes in KB with a thousands separator. */
fun formatSizeKb(kb: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(kb) + " KB"

/** Windows Explorer status bar shows the selected total with one decimal. */
fun formatTotal(kb: Long): String = when {
    kb >= 1024L * 1024L -> String.format(Locale.US, "%.1f GB", kb / (1024.0 * 1024.0))
    kb >= 1024L -> String.format(Locale.US, "%.1f MB", kb / 1024.0)
    else -> "$kb KB"
}

/** Static sample content used until the storage layer is connected (stage 2). */
object SampleData {
    val downloads: List<ExplorerItem> = listOf(
        ExplorerItem("דוח רבעוני Q3.pdf", ItemKind.Pdf, "26/11/2026 03:14", 1204, DateGroup.Today),
        ExplorerItem("Holiday photos.zip", ItemKind.Zip, "26/11/2026 01:52", 48210, DateGroup.Today, selected = true),
        ExplorerItem("installer-1.4.2.apk", ItemKind.Apk, "25/11/2026 22:07", 18344, DateGroup.Today),
        ExplorerItem("הקלטה 2026-11-23.m4a", ItemKind.Audio, "23/11/2026 18:40", 5320, DateGroup.EarlierThisWeek),
        ExplorerItem("Screenshots", ItemKind.Folder, "22/11/2026 09:12", null, DateGroup.EarlierThisWeek),
        ExplorerItem("notes.txt", ItemKind.Text, "21/11/2026 20:31", 3, DateGroup.EarlierThisWeek),
        ExplorerItem("Lecture 04.mp4", ItemKind.Video, "11/11/2026 14:05", 312004, DateGroup.EarlierThisMonth),
        ExplorerItem("חוזה שכירות.docx", ItemKind.Document, "08/11/2026 11:20", 86, DateGroup.EarlierThisMonth),
        ExplorerItem("wallpaper.jpg", ItemKind.Image, "03/11/2026 07:44", 2304, DateGroup.EarlierThisMonth),
    )
}

/** "532 B", "1.2 MB" style size for dialogs (the details list keeps the Windows "1,204 KB" style). */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format(java.util.Locale.US, "%.1f %s", v, units[i])
}

enum class ViewMode { Details, List, Tiles, Icons }

enum class GroupBy { None, Date, Type }

/** Rows split into groups; the first item of a group stands for its header. None means one headerless group. */
internal fun groupItems(items: List<ExplorerItem>, groupBy: GroupBy): List<Pair<ExplorerItem?, List<ExplorerItem>>> = when (groupBy) {
    GroupBy.None -> if (items.isEmpty()) emptyList() else listOf<Pair<ExplorerItem?, List<ExplorerItem>>>(null to items)
    GroupBy.Date -> items.groupBy { it.group }.toSortedMap().values.map { it.first() to it }
    GroupBy.Type -> items.groupBy { it.kind }.toSortedMap().values.map { it.first() to it }
}

/** Index of [path] in a flat list that has one header row per group, or -1. */
internal fun flatIndexOf(groups: List<Pair<ExplorerItem?, List<ExplorerItem>>>, path: String): Int {
    var index = 0
    groups.forEach { (header, rows) ->
        if (header != null) index++
        rows.forEach { if (it.path == path) return index; index++ }
    }
    return -1
}

/** The name shown in lists: without the extension when "Hide extensions" is on (folders and dot-files keep theirs). */
fun displayName(item: ExplorerItem, hideExtensions: Boolean): String {
    if (!hideExtensions || item.kind == ItemKind.Folder) return item.name
    val dot = item.name.lastIndexOf('.')
    return if (dot > 0) item.name.substring(0, dot) else item.name
}
