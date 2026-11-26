package com.droidexplorer.storage.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageApiTest {
    private fun item(name: String, dir: Boolean = false, size: Long = 0, mod: Long = 0) =
        StorageItem("/x/$name", name, dir, size, mod, false)

    @Test fun naturalOrderHandlesNumbers() {
        val names = listOf("file10", "file2", "File1").sortedWith { a, b -> Sorting.naturalCompare(a, b) }
        assertEquals(listOf("File1", "file2", "file10"), names)
    }

    @Test fun foldersStayOnTopInBothDirections() {
        val items = listOf(item("b.txt"), item("a", dir = true), item("c.txt"))
        assertEquals("a", Sorting.sort(items, SortOrder(SortKey.NAME, true)).first().name)
        assertEquals("a", Sorting.sort(items, SortOrder(SortKey.NAME, false)).first().name)
        assertEquals("b.txt", Sorting.sort(items, SortOrder(SortKey.NAME, false)).last().name)
    }

    @Test fun sortBySize() {
        val items = listOf(item("a", size = 5), item("b", size = 1))
        assertEquals("b", Sorting.sort(items, SortOrder(SortKey.SIZE)).first().name)
    }

    @Test fun sameFolderCopyNaming() {
        val taken = mutableSetOf("a.txt")
        val first = NameConflict.sameFolderCopyName("a.txt") { it in taken }
        assertEquals("a - עותק.txt", first)
        taken += first
        assertEquals("a - עותק (2).txt", NameConflict.sameFolderCopyName("a.txt") { it in taken })
    }

    @Test fun keepBothNaming() {
        val taken = setOf("a.txt", "a (1).txt")
        assertEquals("a (2).txt", NameConflict.keepBothName("a.txt") { it in taken })
        assertEquals("b.txt", NameConflict.keepBothName("b.txt") { it in taken })
    }

    @Test fun nameRules() {
        assertNull(NameRules.validate("ok.txt"))
        assertTrue(NameRules.validate("") != null)
        assertTrue(NameRules.validate("a/b") != null)
        assertTrue(NameRules.validate("..") != null)
    }
}
