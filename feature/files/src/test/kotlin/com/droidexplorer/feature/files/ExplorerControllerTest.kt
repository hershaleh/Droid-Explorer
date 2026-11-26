package com.droidexplorer.feature.files

import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.local.LocalStorageProvider
import java.io.File
import java.nio.file.Files
import java.util.Calendar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExplorerControllerTest {
    private lateinit var root: File

    @Before fun setUp() {
        root = Files.createTempDirectory("de-ctl").toFile()
        File(root, "a/b").mkdirs()
        File(root, "a/f.txt").writeText("x")
    }

    @After fun tearDown() { root.deleteRecursively() }

    private fun controller() = ExplorerController(LocalStorageProvider(), root.path)

    @Test fun navigationHistoryAndUp() {
        val c = controller()
        assertFalse(c.canBack); assertFalse(c.canUp)
        c.navigate(File(root, "a").path)
        c.navigate(File(root, "a/b").path)
        assertTrue(c.canBack && c.canUp)
        c.goBack()
        assertEquals(File(root, "a").path, c.currentPath)
        assertTrue(c.canForward)
        c.goForward()
        assertEquals(File(root, "a/b").path, c.currentPath)
        c.goUp(); c.goUp()
        assertEquals(root.path, c.currentPath)
        assertFalse(c.canUp)
    }

    @Test fun newNavigationClearsForwardAndSelection() {
        val c = controller()
        c.navigate(File(root, "a").path)
        c.toggleSelect("x")
        c.goBack()
        assertTrue(c.selection.isEmpty())
        c.navigate(File(root, "a/b").path)
        assertFalse(c.canForward)
    }

    @Test fun loadListsFoldersFirstAndReportsErrors() {
        val c = controller()
        c.navigate(File(root, "a").path)
        c.load()
        assertEquals(listOf("b", "f.txt"), c.items.map { it.name })
        c.navigate(File(root, "missing").path)
        c.load()
        assertTrue(c.items.isEmpty())
        assertTrue(c.error != null)
    }

    @Test fun crumbPathsFollowTheCurrentFolder() {
        val c = controller()
        c.navigate(File(root, "a/b").path)
        assertEquals(listOf(root.path, File(root, "a").path, File(root, "a/b").path), c.crumbPaths())
    }

    @Test fun kindsAndSizes() {
        fun item(n: String, dir: Boolean = false, size: Long = 0) = StorageItem("/x/$n", n, dir, size, 0, false)
        assertEquals(ItemKind.Folder, ItemMapping.kindOf(item("d", true)))
        assertEquals(ItemKind.Pdf, ItemMapping.kindOf(item("A.PDF")))
        assertEquals(ItemKind.Apk, ItemMapping.kindOf(item("x.apk")))
        assertEquals(1L, ItemMapping.toRow(item("t.txt", size = 1), 0, false).sizeKb)
        assertEquals(null, ItemMapping.toRow(item("d", true), 0, false).sizeKb)
    }

    @Test fun dateGroupsFollowWindowsRules() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 26, 12, 0, 0) }.timeInMillis
        fun at(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { set(y, m, d, 9, 0, 0) }.timeInMillis
        assertEquals(DateGroup.Today, ItemMapping.groupOf(at(2026, Calendar.NOVEMBER, 26), now))
        assertEquals(DateGroup.Yesterday, ItemMapping.groupOf(at(2026, Calendar.NOVEMBER, 25), now))
        // 22 Nov 2026 is a Sunday, the start of the current week.
        assertEquals(DateGroup.EarlierThisWeek, ItemMapping.groupOf(at(2026, Calendar.NOVEMBER, 23), now))
        assertEquals(DateGroup.LastWeek, ItemMapping.groupOf(at(2026, Calendar.NOVEMBER, 18), now))
        assertEquals(DateGroup.EarlierThisMonth, ItemMapping.groupOf(at(2026, Calendar.NOVEMBER, 2), now))
        assertEquals(DateGroup.LastMonth, ItemMapping.groupOf(at(2026, Calendar.OCTOBER, 10), now))
        assertEquals(DateGroup.EarlierThisYear, ItemMapping.groupOf(at(2026, Calendar.MARCH, 1), now))
        assertEquals(DateGroup.LongAgo, ItemMapping.groupOf(at(2024, Calendar.MARCH, 1), now))
    }
}
