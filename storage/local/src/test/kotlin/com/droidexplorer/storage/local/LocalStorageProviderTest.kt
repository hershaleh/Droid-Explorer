package com.droidexplorer.storage.local

import com.droidexplorer.storage.api.StorageException
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class LocalStorageProviderTest {
    private lateinit var root: File
    private val p = LocalStorageProvider()

    @Before fun setUp() { root = Files.createTempDirectory("de-test").toFile() }
    @After fun tearDown() { root.deleteRecursively() }

    private fun file(rel: String, text: String = "x"): File =
        File(root, rel).also { it.parentFile.mkdirs(); it.writeText(text) }

    @Test fun listHidesDotFilesByDefault() {
        file("a.txt"); file(".secret")
        assertEquals(listOf("a.txt"), p.list(root.path).map { it.name })
        assertEquals(2, p.list(root.path, includeHidden = true).size)
    }

    @Test fun createFolderRefusesExisting() {
        p.createFolder(root.path, "d")
        try { p.createFolder(root.path, "d"); fail() } catch (_: StorageException) {}
    }

    @Test fun renameRefusesCollisionAndBadNames() {
        file("a.txt"); file("b.txt")
        try { p.rename(File(root, "a.txt").path, "b.txt"); fail() } catch (_: StorageException) {}
        try { p.rename(File(root, "a.txt").path, "x/y"); fail() } catch (_: StorageException) {}
        assertEquals("c.txt", p.rename(File(root, "a.txt").path, "c.txt").name)
        assertTrue(File(root, "b.txt").exists())
    }

    @Test fun copySameFolderUsesWindowsName() {
        file("a.txt", "hello")
        val copy = p.copy(File(root, "a.txt").path, root.path)
        assertEquals("a - עותק.txt", copy.name)
        assertEquals("hello", File(copy.path).readText())
        assertTrue(File(root, "a.txt").exists())
    }

    @Test fun copyToOtherFolderKeepsBothNeverOverwrites() {
        file("a.txt", "new"); file("dst/a.txt", "old")
        val copy = p.copy(File(root, "a.txt").path, File(root, "dst").path)
        assertEquals("a (1).txt", copy.name)
        assertEquals("old", File(root, "dst/a.txt").readText())
    }

    @Test fun copyFolderRecursivelyAndNotIntoItself() {
        file("d/sub/f.txt", "z")
        p.createFolder(root.path, "t")
        p.copy(File(root, "d").path, File(root, "t").path)
        assertEquals("z", File(root, "t/d/sub/f.txt").readText())
        try { p.copy(File(root, "d").path, File(root, "d/sub").path); fail() } catch (_: StorageException) {}
    }

    @Test fun moveRemovesSource() {
        file("a.txt"); p.createFolder(root.path, "t")
        val moved = p.move(File(root, "a.txt").path, File(root, "t").path)
        assertEquals("a.txt", moved.name)
        assertFalse(File(root, "a.txt").exists())
    }

    @Test fun deleteFolderRecursive() {
        file("d/x/y.txt")
        p.delete(File(root, "d").path)
        assertFalse(File(root, "d").exists())
        try { p.delete(File(root, "d").path); fail() } catch (_: StorageException) {}
    }
}
