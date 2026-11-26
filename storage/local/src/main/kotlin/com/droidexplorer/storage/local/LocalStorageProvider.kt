package com.droidexplorer.storage.local

import com.droidexplorer.core.capabilities.Capability
import com.droidexplorer.core.capabilities.CapabilitySet
import com.droidexplorer.storage.api.NameConflict
import com.droidexplorer.storage.api.NameRules
import com.droidexplorer.storage.api.StorageException
import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.api.StorageProvider
import java.io.File

/**
 * Local file system provider built on java.io.File. No trash here: permanent delete is the only delete
 * until the app-own recycle bin exists, so the UI must confirm before calling [delete].
 */
class LocalStorageProvider : StorageProvider {
    override val capabilities: CapabilitySet = CapabilitySet.of(
        Capability.READ, Capability.WRITE, Capability.CREATE, Capability.RENAME,
        Capability.MOVE, Capability.COPY, Capability.DELETE_PERMANENT, Capability.LIST_CHILDREN,
    )

    override fun list(path: String, includeHidden: Boolean): List<StorageItem> {
        val dir = File(path)
        if (!dir.isDirectory) throw StorageException("Not a folder: $path")
        val children = dir.listFiles() ?: throw StorageException("Cannot read folder: $path")
        return children.filter { includeHidden || !it.isHidden }.map { it.toItem() }
    }

    override fun createFolder(parent: String, name: String): StorageItem {
        checkName(name)
        val target = File(parent, name)
        if (target.exists()) throw StorageException("Already exists: $name")
        if (!target.mkdir()) throw StorageException("Cannot create: $name")
        return target.toItem()
    }

    override fun rename(path: String, newName: String): StorageItem {
        checkName(newName)
        val src = File(path)
        if (!src.exists()) throw StorageException("Missing: $path")
        val dst = File(src.parentFile, newName)
        if (dst.exists() && !dst.canonicalFile.equals(src.canonicalFile)) throw StorageException("Already exists: $newName")
        if (!src.renameTo(dst)) throw StorageException("Cannot rename: $path")
        return dst.toItem()
    }

    override fun copy(path: String, targetFolder: String): StorageItem {
        val src = File(path)
        val folder = File(targetFolder)
        if (!src.exists()) throw StorageException("Missing: $path")
        if (!folder.isDirectory) throw StorageException("Not a folder: $targetFolder")
        if (src.isDirectory && isInside(folder, src)) throw StorageException("Cannot copy a folder into itself")
        val sameFolder = src.parentFile.canonicalFile == folder.canonicalFile
        val name = if (sameFolder) {
            NameConflict.sameFolderCopyName(src.name) { File(folder, it).exists() }
        } else {
            NameConflict.keepBothName(src.name) { File(folder, it).exists() }
        }
        val dst = File(folder, name)
        src.copyRecursively(dst, overwrite = false) { _, e -> throw StorageException("Copy failed", e) }
        return dst.toItem()
    }

    override fun move(path: String, targetFolder: String): StorageItem {
        val src = File(path)
        val folder = File(targetFolder)
        if (!src.exists()) throw StorageException("Missing: $path")
        if (!folder.isDirectory) throw StorageException("Not a folder: $targetFolder")
        if (src.isDirectory && isInside(folder, src)) throw StorageException("Cannot move a folder into itself")
        val name = NameConflict.keepBothName(src.name) { File(folder, it).exists() }
        val dst = File(folder, name)
        if (!src.renameTo(dst)) {
            // Different volumes: copy then delete, only deleting the source after a full successful copy.
            src.copyRecursively(dst, overwrite = false) { _, e -> throw StorageException("Move failed", e) }
            if (!src.deleteRecursively()) throw StorageException("Copied but could not remove the source: $path")
        }
        return dst.toItem()
    }

    override fun delete(path: String) {
        val f = File(path)
        if (!f.exists()) throw StorageException("Missing: $path")
        if (!f.deleteRecursively()) throw StorageException("Cannot delete: $path")
    }

    private fun checkName(name: String) {
        NameRules.validate(name)?.let { throw StorageException("Invalid name ($it): $name") }
    }

    private fun isInside(child: File, parent: File): Boolean {
        val c = child.canonicalFile.toPath()
        return c.startsWith(parent.canonicalFile.toPath())
    }

    private fun File.toItem() = StorageItem(
        path = path,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0 else length(),
        modifiedMillis = lastModified(),
        isHidden = isHidden || name.startsWith("."),
    )
}
