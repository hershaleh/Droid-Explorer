package com.droidexplorer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.droidexplorer.feature.files.LiveExplorerScreen
import com.droidexplorer.feature.files.VolumeInfo
import com.droidexplorer.storage.local.LocalStorageProvider
import com.droidexplorer.ui.design.DroidExplorerTheme
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ButtonStyle
import com.droidexplorer.ui.design.controls.FluentButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidExplorerTheme {
                StorageGate {
                    val provider = remember { LocalStorageProvider() }
                    val context = LocalContext.current
                    val volumes = remember { context.storageVolumes() }
                    LiveExplorerScreen(
                        provider = provider,
                        rootPath = volumes.first().path,
                        volumes = volumes,
                        onOpenFile = { context.openFile(it) },
                        onShare = { context.shareFiles(it) },
                    )
                }
            }
        }
    }
}

/** Internal storage first, then SD cards and USB drives that are currently mounted. */
private fun android.content.Context.storageVolumes(): List<VolumeInfo> {
    val primary = Environment.getExternalStorageDirectory().path
    val sm = getSystemService(android.os.storage.StorageManager::class.java)
    val roots = getExternalFilesDirs(null).filterNotNull()
        .map { it.path.substringBefore("/Android/data") }
        .filter { it != primary && File(it).isDirectory }
        .distinct()
    val result = mutableListOf(VolumeInfo(getString(R.string.volume_internal), primary))
    roots.forEach { root ->
        val vol = try { sm?.getStorageVolume(File(root)) } catch (_: Exception) { null }
        result += VolumeInfo(vol?.getDescription(this) ?: File(root).name, root, removable = true)
    }
    return result
}

private fun android.content.Context.uriFor(path: String): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", File(path))

private fun mimeOf(path: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(path).extension.lowercase()) ?: "*/*"

private fun android.content.Context.openFile(path: String) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uriFor(path), mimeOf(path))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: Exception) {
        android.widget.Toast.makeText(this, getString(R.string.no_app_to_open), android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun android.content.Context.shareFiles(paths: List<String>) {
    if (paths.isEmpty()) return
    val files = paths.filter { File(it).isFile }
    if (files.isEmpty()) {
        android.widget.Toast.makeText(this, getString(R.string.share_files_only), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    val intent = if (files.size == 1) {
        Intent(Intent.ACTION_SEND).setType(mimeOf(files[0])).putExtra(Intent.EXTRA_STREAM, uriFor(files[0]))
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files.map { uriFor(it) }))
    }
    startActivity(Intent.createChooser(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
}

private fun hasStorageAccess(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

/** Shows [content] once file access is granted; otherwise explains why it is needed and opens the right screen. */
@Composable
private fun StorageGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasStorageAccess(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasStorageAccess(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasStorageAccess(context)
    }
    if (granted) {
        content()
        return
    }
    val c = FluentTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.layer).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.perm_title), style = FluentTheme.typography.subtitle, color = c.textPrimary)
        Text(stringResource(R.string.perm_body), style = FluentTheme.typography.body, color = c.textSecondary)
        FluentButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    legacyLauncher.launch(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    )
                }
            },
            style = ButtonStyle.Accent,
        ) { Text(stringResource(R.string.perm_button)) }
    }
}
