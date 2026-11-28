package com.droidexplorer.storage.local

import com.droidexplorer.storage.api.Conflict
import com.droidexplorer.storage.api.ConflictChoice
import com.droidexplorer.storage.api.ConflictDecision
import com.droidexplorer.storage.api.TransferListener
import com.droidexplorer.storage.api.TransferProgress
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileTransferTest {
    private lateinit var root: File
    private val p = LocalStorageProvider()

    @Before fun setUp() {
        root = Files.createTempDirectory("de-tr").toFile()
        File(root, "dst").mkdirs()
    }

    @After fun tearDown() { root.deleteRecursively() }

    private fun file(rel: String, text: String) = File(root, rel).also { it.parentFile.mkdirs(); it.writeText(text) }

    private class Ask(val answer: ConflictDecision?) : TransferListener {
        var asked = 0
        var last: TransferProgress? = null
        override fun onConflict(conflict: Conflict): ConflictDecision? { asked++; return answer }
        override fun onProgress(progress: TransferProgress) { last = progress }
    }

    @Test fun conflictReplaceOverwritesWithNewContent() {
        val src = file("a.txt", "new"); file("dst/a.txt", "old")
        val l = Ask(ConflictDecision(ConflictChoice.REPLACE))
        val r = p.transfer(listOf(src.path), File(root, "dst").path, false, l)
        assertEquals("new", File(root, "dst/a.txt").readText())
        assertEquals(1, l.asked)
        assertEquals(listOf(File(root, "dst/a.txt").path), r.created)
        assertTrue(src.exists())
    }

    @Test fun conflictSkipKeepsOldAndCountsSkipped() {
        val src = file("a.txt", "new"); file("dst/a.txt", "old")
        val r = p.transfer(listOf(src.path), File(root, "dst").path, false, Ask(ConflictDecision(ConflictChoice.SKIP)))
        assertEquals("old", File(root, "dst/a.txt").readText())
        assertEquals(1, r.skipped)
        assertTrue(r.created.isEmpty())
    }

    @Test fun conflictKeepBothAddsNumber() {
        val src = file("a.txt", "new"); file("dst/a.txt", "old")
        p.transfer(listOf(src.path), File(root, "dst").path, false, Ask(ConflictDecision(ConflictChoice.KEEP_BOTH)))
        assertEquals("old", File(root, "dst/a.txt").readText())
        assertEquals("new", File(root, "dst/a (1).txt").readText())
    }

    @Test fun skipIdenticalSkipsSameSizeAndDateWithoutAsking() {
        val a = file("a.txt", "same"); val b = file("b.txt", "bbb")
        val da = file("dst/a.txt", "same"); file("dst/b.txt", "different size")
        da.setLastModified(a.lastModified())
        val l = Ask(ConflictDecision(ConflictChoice.SKIP, skipIdentical = true))
        val r = p.transfer(listOf(b.path, a.path), File(root, "dst").path, false, l)
        assertEquals(1, l.asked)
        assertEquals(2, r.skipped)
    }

    @Test fun pausedTransferWaitsUntilResumed() {
        val src = file("big.txt", "x".repeat(1000))
        val started = System.nanoTime()
        val l = object : TransferListener {
            override fun isPaused() = System.nanoTime() - started < 300_000_000L
        }
        p.transfer(listOf(src.path), File(root, "dst2").also { it.mkdirs() }.path, false, l)
        assertTrue((System.nanoTime() - started) >= 250_000_000L)
        assertTrue(File(root, "dst2/big.txt").exists())
    }

    @Test fun applyToAllAsksOnlyOnce() {
        val a = file("a.txt", "A2"); val b = file("b.txt", "B2")
        file("dst/a.txt", "A"); file("dst/b.txt", "B")
        val l = Ask(ConflictDecision(ConflictChoice.REPLACE, applyToAll = true))
        p.transfer(listOf(a.path, b.path), File(root, "dst").path, false, l)
        assertEquals(1, l.asked)
        assertEquals("A2", File(root, "dst/a.txt").readText())
        assertEquals("B2", File(root, "dst/b.txt").readText())
    }

    @Test fun cancellingTheQuestionCancelsAndLeavesFilesAlone() {
        val src = file("a.txt", "new"); file("dst/a.txt", "old")
        val r = p.transfer(listOf(src.path), File(root, "dst").path, false, Ask(null))
        assertTrue(r.cancelled)
        assertEquals("old", File(root, "dst/a.txt").readText())
    }

    @Test fun foldersMergeAndOnlyConflictingFilesAsk() {
        file("d/x.txt", "x"); file("d/y.txt", "y2")
        file("dst/d/y.txt", "y"); file("dst/d/z.txt", "z")
        val l = Ask(ConflictDecision(ConflictChoice.SKIP))
        p.transfer(listOf(File(root, "d").path), File(root, "dst").path, false, l)
        assertEquals(1, l.asked)
        assertEquals("x", File(root, "dst/d/x.txt").readText())
        assertEquals("y", File(root, "dst/d/y.txt").readText())
        assertEquals("z", File(root, "dst/d/z.txt").readText())
    }

    @Test fun sameFolderCopyNeverAsksAndUsesWindowsName() {
        val src = file("a.txt", "hi")
        val l = Ask(null)
        val r = p.transfer(listOf(src.path), root.path, false, l)
        assertEquals(0, l.asked)
        assertEquals(listOf(File(root, "a - עותק.txt").path), r.created)
    }

    @Test fun moveRemovesSourceAndMergesFolders() {
        file("d/x.txt", "x"); file("dst/d/y.txt", "y")
        p.transfer(listOf(File(root, "d").path), File(root, "dst").path, true, Ask(null))
        assertFalse(File(root, "d").exists())
        assertEquals("x", File(root, "dst/d/x.txt").readText())
        assertEquals("y", File(root, "dst/d/y.txt").readText())
    }

    @Test fun progressReachesTotals() {
        val big = File(root, "big.bin").also { it.writeBytes(ByteArray(1_000_000)) }
        val l = Ask(null)
        p.transfer(listOf(big.path), File(root, "dst").path, false, l)
        val last = l.last!!
        assertEquals(1_000_000L, last.doneBytes)
        assertEquals(1_000_000L, last.totalBytes)
        assertEquals(1, last.doneItems)
    }

    @Test fun cancelStopsAndRemovesPartialFile() {
        val big = File(root, "big.bin").also { it.writeBytes(ByteArray(3_000_000)) }
        var calls = 0
        val l = object : TransferListener {
            override fun isCancelled(): Boolean = ++calls > 3
        }
        val r = p.transfer(listOf(big.path), File(root, "dst").path, false, l)
        assertTrue(r.cancelled)
        assertEquals(emptyList<String>(), File(root, "dst").list()!!.toList())
    }

    @Test fun folderIntoItselfIsRefused() {
        file("d/x.txt", "x")
        val sub = File(root, "d/sub").also { it.mkdirs() }
        try {
            p.transfer(listOf(File(root, "d").path), sub.path, false, Ask(null))
            org.junit.Assert.fail()
        } catch (_: com.droidexplorer.storage.api.StorageException) {}
    }
}
