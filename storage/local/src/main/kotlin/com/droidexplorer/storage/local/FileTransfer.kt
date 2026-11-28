package com.droidexplorer.storage.local

import com.droidexplorer.storage.api.Conflict
import com.droidexplorer.storage.api.ConflictChoice
import com.droidexplorer.storage.api.NameConflict
import com.droidexplorer.storage.api.StorageException
import com.droidexplorer.storage.api.TransferListener
import com.droidexplorer.storage.api.TransferProgress
import com.droidexplorer.storage.api.TransferResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private class CancelledException : Exception()

/**
 * One copy/move operation. Files are copied to a temporary name and renamed into place, so a cancelled or
 * failed copy never leaves a half-written file under the real name and a replace never loses the old file early.
 */
internal class FileTransfer(
    private val provider: LocalStorageProvider,
    private val sources: List<File>,
    private val target: File,
    private val move: Boolean,
    private val listener: TransferListener,
) {
    private var totalBytes = 0L
    private var totalItems = 0
    private var doneBytes = 0L
    private var doneItems = 0
    private var skipped = 0
    private var sticky: ConflictChoice? = null
    private var skipIdentical = false
    private var lastReport = 0L
    private val started = System.nanoTime()
    private val created = mutableListOf<String>()

    fun run(): TransferResult {
        if (!target.isDirectory) throw StorageException("Not a folder: ${target.path}")
        sources.forEach { src ->
            if (!src.exists()) throw StorageException("Missing: ${src.path}")
            if (src.isDirectory && target.canonicalFile.toPath().startsWith(src.canonicalFile.toPath())) {
                throw StorageException("Cannot copy or move a folder into itself")
            }
            measure(src)
        }
        try {
            sources.forEach { transferTop(it) }
        } catch (_: CancelledException) {
            report("", force = true)
            return TransferResult(created.toList(), skipped, cancelled = true)
        }
        report("", force = true)
        return TransferResult(created.toList(), skipped, cancelled = false)
    }

    private fun measure(f: File) {
        if (f.isDirectory) {
            totalItems++
            f.listFiles()?.forEach { measure(it) }
        } else {
            totalItems++
            totalBytes += f.length()
        }
    }

    private fun transferTop(src: File) {
        val sameFolder = src.parentFile?.canonicalFile == target.canonicalFile
        if (sameFolder && move) {
            count(src)
            return
        }
        val name = if (sameFolder) {
            NameConflict.sameFolderCopyName(src.name) { File(target, it).exists() }
        } else {
            src.name
        }
        val dst = File(target, name)
        val result = transferNode(src, dst, topLevel = true)
        if (result != null) created += result.path
    }

    /** Returns the final destination, or null when the item was skipped. */
    private fun transferNode(src: File, dstWanted: File, topLevel: Boolean): File? {
        checkCancelled()
        var dst = dstWanted
        if (src.isDirectory) {
            if (dst.exists() && !dst.isDirectory) {
                dst = File(dst.parentFile, NameConflict.keepBothName(dst.name) { File(dst.parentFile, it).exists() })
            }
            if (!dst.exists() && move) {
                val items = itemCount(src)
                val bytes = byteCount(src)
                if (src.renameTo(dst)) {
                    doneItems += items
                    doneBytes += bytes
                    report(dst.name, force = true)
                    return dst
                }
            }
            if (!dst.exists() && !dst.mkdirs()) throw StorageException("Cannot create: ${dst.path}")
            doneItems++
            src.listFiles()?.forEach { child -> transferNode(child, File(dst, child.name), topLevel = false) }
            if (move) src.delete() // only succeeds when everything inside was moved
            return dst
        }
        if (dst.exists()) {
            val choice = decide(src, dst)
            when (choice) {
                ConflictChoice.SKIP -> {
                    skipped++
                    doneBytes += src.length()
                    doneItems++
                    report(src.name)
                    return null
                }
                ConflictChoice.KEEP_BOTH -> {
                    val parent = dst.parentFile
                    dst = File(parent, NameConflict.keepBothName(dst.name) { File(parent, it).exists() })
                }
                ConflictChoice.REPLACE -> Unit
            }
        }
        copyFile(src, dst)
        if (move && src.exists() && !src.delete()) throw StorageException("Copied but could not remove the source: ${src.path}")
        return dst
    }

    private fun decide(src: File, dst: File): ConflictChoice {
        sticky?.let { return it }
        if (skipIdentical && src.isFile && dst.isFile && src.length() == dst.length() &&
            Math.abs(src.lastModified() - dst.lastModified()) < 2000
        ) return ConflictChoice.SKIP
        val decision = listener.onConflict(Conflict(provider.itemOf(src), provider.itemOf(dst), totalItems))
            ?: throw CancelledException()
        if (decision.skipIdentical) skipIdentical = true
        if (decision.applyToAll) sticky = decision.choice
        return decision.choice
    }

    private fun copyFile(src: File, dst: File) {
        val sameVolumeMove = move && !dst.exists() && src.renameTo(dst)
        if (sameVolumeMove) {
            doneBytes += dst.length()
            doneItems++
            report(dst.name, force = true)
            return
        }
        val tmp = File(dst.parentFile, ".${dst.name}.part")
        try {
            FileInputStream(src).use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        checkCancelled()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        doneBytes += n
                        report(src.name)
                    }
                }
            }
            tmp.setLastModified(src.lastModified())
            Files.move(tmp.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: CancelledException) {
            tmp.delete()
            throw e
        } catch (e: Exception) {
            tmp.delete()
            throw StorageException("Copy failed: ${src.name}", e)
        }
        doneItems++
        report(src.name, force = true)
    }

    private fun count(f: File) {
        doneItems += itemCount(f)
        doneBytes += byteCount(f)
    }

    private fun itemCount(f: File): Int = 1 + (f.listFiles()?.sumOf { itemCount(it) } ?: 0)

    private fun byteCount(f: File): Long = if (f.isDirectory) (f.listFiles()?.sumOf { byteCount(it) } ?: 0L) else f.length()

    private fun checkCancelled() {
        while (listener.isPaused() && !listener.isCancelled()) Thread.sleep(80)
        if (listener.isCancelled()) throw CancelledException()
    }

    private fun report(name: String, force: Boolean = false) {
        val now = System.nanoTime()
        if (!force && now - lastReport < 100_000_000L) return
        lastReport = now
        val seconds = (now - started) / 1e9
        val speed = if (seconds > 0.2) (doneBytes / seconds).toLong() else 0L
        listener.onProgress(TransferProgress(name, doneBytes, totalBytes, doneItems, totalItems, speed))
    }
}
