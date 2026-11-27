package com.droidexplorer.feature.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.droidexplorer.storage.api.Conflict
import com.droidexplorer.storage.api.ConflictChoice
import com.droidexplorer.storage.api.StorageItem
import com.droidexplorer.storage.api.TransferProgress
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ExplorerDialog
import com.droidexplorer.ui.design.controls.ExplorerDialogButton
import com.droidexplorer.ui.design.controls.ExplorerDialogHeading
import com.droidexplorer.ui.design.controls.FluentCheckBox
import com.droidexplorer.ui.design.controls.FluentProgressBar
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

/** A plain message with one OK button, like the File Explorer error boxes. */
@Composable
fun ClassicMessageDialog(title: String, message: String, onOk: () -> Unit) {
    ExplorerDialog(
        title = title,
        onDismissRequest = onOk,
        buttons = { ExplorerDialogButton(stringResource(R.string.dlg_ok), onOk, isDefault = true) },
    ) {
        Text(message, style = FluentTheme.typography.body, color = FluentTheme.colors.textPrimary)
    }
}

/** Yes/No question; Yes is the default button. */
@Composable
fun ClassicQuestionDialog(title: String, question: String, onYes: () -> Unit, onNo: () -> Unit, content: @Composable () -> Unit = {}) {
    ExplorerDialog(
        title = title,
        onDismissRequest = onNo,
        buttons = {
            ExplorerDialogButton(stringResource(R.string.yes), onYes, isDefault = true)
            ExplorerDialogButton(stringResource(R.string.no), onNo)
        },
    ) {
        Text(question, style = FluentTheme.typography.body, color = FluentTheme.colors.textPrimary)
        content()
    }
}

/** "Are you sure you want to permanently delete ...", with the item's details for a single item. */
@Composable
fun DeleteConfirmDialog(items: List<StorageItem>, onYes: () -> Unit, onNo: () -> Unit) {
    val single = items.singleOrNull()
    val title = when {
        single == null -> R.string.delete_title_multi
        single.isDirectory -> R.string.delete_title_folder
        else -> R.string.delete_title_file
    }
    val question = when {
        single == null -> stringResource(R.string.delete_q_multi, items.size)
        single.isDirectory -> stringResource(R.string.delete_q_folder)
        else -> stringResource(R.string.delete_q_file)
    }
    ClassicQuestionDialog(stringResource(title), question, onYes, onNo) {
        if (single != null) {
            val c = FluentTheme.colors
            val style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileGlyph(ItemMapping.kindOf(single), glyphSize = 40.dp)
                Column {
                    Text(single.name, style = FluentTheme.typography.bodyStrong, color = c.textPrimary, maxLines = 2)
                    Text(stringResource(R.string.item_type, ItemMapping.kindOf(single).typeLabel()), style = style, color = c.textSecondary)
                    if (!single.isDirectory) Text(stringResource(R.string.item_size, formatBytes(single.sizeBytes)), style = style, color = c.textSecondary)
                    Text(stringResource(R.string.item_modified, ItemMapping.format(single.modifiedMillis)), style = style, color = c.textSecondary)
                }
            }
        }
    }
}

/** The File Explorer copy/move window: percent in the title, blue heading, progress bar with cancel, details. */
@Composable
fun TransferProgressDialog(ui: TransferUi, onCancel: () -> Unit) {
    val c = FluentTheme.colors
    val p = ui.progress
    val percent = if (p.totalBytes > 0) ((p.doneBytes * 100) / p.totalBytes).toInt().coerceIn(0, 100)
    else if (p.totalItems > 0) (p.doneItems * 100 / p.totalItems).coerceIn(0, 100) else 0
    var more by remember { mutableStateOf(false) }
    val text = FluentTheme.typography.body.copy(textDirection = TextDirection.Content)
    val remainingBytes = (p.totalBytes - p.doneBytes).coerceAtLeast(0)
    val seconds = if (p.bytesPerSecond > 0) remainingBytes / p.bytesPerSecond else -1L
    val time = when {
        seconds < 0 -> stringResource(R.string.time_calculating)
        seconds < 60 -> stringResource(R.string.time_seconds, seconds.toInt().coerceAtLeast(1))
        else -> stringResource(R.string.time_minutes, (seconds / 60).toInt())
    }
    ExplorerDialog(title = stringResource(R.string.transfer_title, percent), onDismissRequest = {}) {
        val line = if (ui.moving) R.string.transfer_moving else R.string.transfer_copying
        ExplorerDialogHeading(stringResource(line, p.totalItems, ui.from, ui.to))
        Text(stringResource(R.string.transfer_title, percent), style = FluentTheme.typography.bodyLarge, color = c.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FluentProgressBar(progress = percent / 100f, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(28.dp).clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) { FluentIcon(FluentIcons.Close, tint = c.textPrimary, size = 12.dp) }
        }
        Text(stringResource(R.string.transfer_name, p.currentName), style = text, color = c.textPrimary, maxLines = 1)
        Text(stringResource(R.string.transfer_time, time), style = text, color = c.textPrimary)
        Text(
            stringResource(R.string.transfer_remaining, (p.totalItems - p.doneItems).coerceAtLeast(0), formatBytes(remainingBytes)),
            style = text, color = c.textPrimary,
        )
        if (more) Text(stringResource(R.string.transfer_speed, formatBytes(p.bytesPerSecond)), style = text, color = c.textPrimary)
        Text(
            if (more) stringResource(R.string.transfer_less) else stringResource(R.string.transfer_more),
            style = FluentTheme.typography.body,
            color = c.accent,
            modifier = Modifier.clickable { more = !more },
        )
    }
}

@Composable
private fun CommandLink(text: String, icon: FluentIcons, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FluentShapes.control)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(color = FluentTheme.colors.textPrimary),
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FluentIcon(icon, tint = Color(0xFF107C10), size = 18.dp)
        Text(text, style = FluentTheme.typography.bodyLarge, color = FluentTheme.colors.textPrimary)
    }
}

/** What the person decided in the replace/skip/compare dialogs. */
data class ConflictAnswer(val choice: ConflictChoice, val applyToAll: Boolean)

/**
 * "Replace or Skip Files" with the three File Explorer choices. "Compare info for both files" opens the
 * "Which files do you want to keep?" window; ticking both keeps both, one keeps only that one.
 */
@Composable
fun ConflictDialogs(conflict: Conflict, totalItems: Int, onAnswer: (ConflictAnswer?) -> Unit) {
    var comparing by remember(conflict) { mutableStateOf(false) }
    var all by remember(conflict) { mutableStateOf(false) }
    val c = FluentTheme.colors
    val fromName = java.io.File(conflict.source.path).parentFile?.name.orEmpty()
    val toName = java.io.File(conflict.existing.path).parentFile?.name.orEmpty()
    if (!comparing) {
        ExplorerDialog(
            title = stringResource(R.string.conflict_title),
            onDismissRequest = { onAnswer(null) },
            buttons = { ExplorerDialogButton(stringResource(R.string.dlg_cancel), { onAnswer(null) }) },
        ) {
            ExplorerDialogHeading(stringResource(R.string.transfer_copying, totalItems, fromName, toName))
            Text(
                stringResource(R.string.conflict_body, conflict.source.name),
                style = FluentTheme.typography.body, color = c.textPrimary,
            )
            CommandLink(stringResource(R.string.conflict_replace), FluentIcons.Check) { onAnswer(ConflictAnswer(ConflictChoice.REPLACE, all)) }
            CommandLink(stringResource(R.string.conflict_skip), FluentIcons.Forward) { onAnswer(ConflictAnswer(ConflictChoice.SKIP, all)) }
            CommandLink(stringResource(R.string.conflict_compare), FluentIcons.View) { comparing = true }
            if (totalItems > 1) {
                FluentCheckBox(checked = all, onCheckedChange = { all = it }, label = stringResource(R.string.conflict_all))
            }
        }
    } else {
        var keepSource by remember(conflict) { mutableStateOf(true) }
        var keepExisting by remember(conflict) { mutableStateOf(true) }
        ExplorerDialog(
            title = stringResource(R.string.compare_title),
            onDismissRequest = { onAnswer(null) },
            buttons = {
                ExplorerDialogButton(
                    stringResource(R.string.compare_continue),
                    {
                        val choice = when {
                            keepSource && keepExisting -> ConflictChoice.KEEP_BOTH
                            keepSource -> ConflictChoice.REPLACE
                            else -> ConflictChoice.SKIP
                        }
                        onAnswer(ConflictAnswer(choice, all))
                    },
                    isDefault = true,
                    enabled = keepSource || keepExisting,
                )
                ExplorerDialogButton(stringResource(R.string.dlg_cancel), { onAnswer(null) })
            },
        ) {
            Text(stringResource(R.string.compare_body), style = FluentTheme.typography.body, color = c.textPrimary)
            CompareCard(stringResource(R.string.compare_from, fromName), conflict.source, keepSource) { keepSource = it }
            CompareCard(stringResource(R.string.compare_in, toName), conflict.existing, keepExisting) { keepExisting = it }
            if (totalItems > 1) {
                FluentCheckBox(checked = all, onCheckedChange = { all = it }, label = stringResource(R.string.conflict_all))
            }
        }
    }
}

@Composable
private fun CompareCard(heading: String, item: StorageItem, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val c = FluentTheme.colors
    val style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FluentCheckBox(checked = checked, onCheckedChange = onChecked, label = heading)
        Row(Modifier.padding(start = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FileGlyph(ItemMapping.kindOf(item), glyphSize = 40.dp)
            Column {
                Text(item.name, style = FluentTheme.typography.bodyStrong, color = c.textPrimary, maxLines = 1)
                Text(stringResource(R.string.compare_size, formatBytes(item.sizeBytes)), style = style, color = c.textSecondary)
                Text(stringResource(R.string.compare_modified, ItemMapping.format(item.modifiedMillis)), style = style, color = c.textSecondary)
            }
        }
    }
}
