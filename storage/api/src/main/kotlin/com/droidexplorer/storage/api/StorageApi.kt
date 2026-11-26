package com.droidexplorer.storage.api

import com.droidexplorer.core.capabilities.CapabilitySet

/** A file or folder as seen by the UI, independent of the underlying storage type. */
data class StorageItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedMillis: Long,
    val isHidden: Boolean,
)

enum class SortKey { NAME, MODIFIED, TYPE, SIZE }

data class SortOrder(val key: SortKey = SortKey.NAME, val ascending: Boolean = true)

class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * A storage backend. The UI asks [capabilities] and shows only the actions the provider declares.
 * Operations never overwrite silently: name collisions are resolved with [NameConflict].
 */
interface StorageProvider {
    val capabilities: CapabilitySet
    fun list(path: String, includeHidden: Boolean = false): List<StorageItem>
    fun createFolder(parent: String, name: String): StorageItem
    fun rename(path: String, newName: String): StorageItem
    fun copy(path: String, targetFolder: String): StorageItem
    fun move(path: String, targetFolder: String): StorageItem
    fun delete(path: String)
}

/** Windows-style sorting: folders first, natural (numeric-aware) name order, case-insensitive. */
object Sorting {
    private val chunk = Regex("(\\d+)|(\\D+)")

    fun naturalKey(name: String): List<String> = chunk.findAll(name).map { it.value }.toList()

    fun compareKeys(ca: List<String>, cb: List<String>): Int {
        for (i in 0 until minOf(ca.size, cb.size)) {
            val x = ca[i]
            val y = cb[i]
            val r = if (x[0].isDigit() && y[0].isDigit()) {
                val nx = x.trimStart('0')
                val ny = y.trimStart('0')
                if (nx.length != ny.length) nx.length.compareTo(ny.length) else nx.compareTo(ny)
            } else {
                x.compareTo(y, ignoreCase = true)
            }
            if (r != 0) return r
        }
        return ca.size.compareTo(cb.size)
    }

    fun naturalCompare(a: String, b: String): Int = compareKeys(naturalKey(a), naturalKey(b))

    fun extension(name: String): String = name.substringAfterLast('.', "").lowercase()

    fun sort(items: List<StorageItem>, order: SortOrder): List<StorageItem> {
        // Keys are computed once per item, not once per comparison (large folders).
        val keys = HashMap<String, List<String>>(items.size * 2)
        items.forEach { keys[it.path] = naturalKey(it.name) }
        val byName = Comparator<StorageItem> { a, b -> compareKeys(keys.getValue(a.path), keys.getValue(b.path)) }
        val base: Comparator<StorageItem> = when (order.key) {
            SortKey.NAME -> byName
            SortKey.MODIFIED -> compareBy<StorageItem> { it.modifiedMillis }.then(byName)
            SortKey.SIZE -> compareBy<StorageItem> { it.sizeBytes }.then(byName)
            SortKey.TYPE -> Comparator<StorageItem> { a, b -> extension(a.name).compareTo(extension(b.name)) }.then(byName)
        }
        val directed = if (order.ascending) base else base.reversed()
        // Folders stay on top in both directions, as in Windows File Explorer.
        return items.sortedWith(compareByDescending<StorageItem> { it.isDirectory }.then(directed))
    }
}

/** Windows File Explorer name-collision rules. */
object NameConflict {
    /** Copy into the same folder: "a.txt" -> "a - עותק.txt", then "a - עותק (2).txt". */
    fun sameFolderCopyName(name: String, taken: (String) -> Boolean): String {
        val (stem, ext) = split(name)
        var candidate = "$stem - עותק$ext"
        var n = 2
        while (taken(candidate)) {
            candidate = "$stem - עותק ($n)$ext"
            n++
        }
        return candidate
    }

    /** Copy/move into another folder when the user chose "keep both": "a (1).txt", "a (2).txt". */
    fun keepBothName(name: String, taken: (String) -> Boolean): String {
        if (!taken(name)) return name
        val (stem, ext) = split(name)
        var n = 1
        while (taken("$stem ($n)$ext")) n++
        return "$stem ($n)$ext"
    }

    private fun split(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0) name to "" else name.substring(0, dot) to name.substring(dot)
    }
}

object NameRules {
    private val forbidden = charArrayOf('/', '\u0000')

    fun validate(name: String): String? = when {
        name.isBlank() -> "empty"
        name == "." || name == ".." -> "reserved"
        name.any { it in forbidden } -> "invalid-char"
        name.toByteArray().size > 255 -> "too-long"
        else -> null
    }
}
