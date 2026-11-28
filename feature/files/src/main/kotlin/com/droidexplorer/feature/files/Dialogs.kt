package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/** Folder or file glyph with the red cross Windows draws in the permanent-delete box. */
@Composable
private fun DeleteGlyph(kind: ItemKind) {
    Box(Modifier.size(48.dp)) {
        if (kind == ItemKind.Folder) FolderGlyph(Modifier.align(Alignment.Center), glyphSize = 44.dp)
        else FileGlyph(kind, glyphSize = 44.dp)
        androidx.compose.foundation.Canvas(Modifier.size(22.dp).align(Alignment.BottomEnd)) {
            val red = Color(0xFFD13438)
            val w = 2.4.dp.toPx()
            drawLine(red, androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.15f), androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.85f), w, androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(red, androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f), androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f), w, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/** "Are you sure you want to permanently delete ...", with the item's glyph and details for a single item. */
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
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DeleteGlyph(ItemMapping.kindOf(single))
                Column {
                    Text(single.name, style = style, color = c.textPrimary, maxLines = 2)
                    Text(stringResource(R.string.item_modified, ItemMapping.format(single.modifiedMillis)), style = style, color = c.textSecondary)
                }
            }
        }
    }
}

/** "Copying N items from [A] to [B]" with the folder names in link colour, as in the Windows copy windows. */
@Composable
private fun transferHeading(moving: Boolean, count: Int, from: String, to: String): AnnotatedString {
    val link = SpanStyle(color = FluentTheme.colors.accent)
    val verb = when {
        moving && count == 1 -> stringResource(R.string.xfer_verb_move_one)
        moving -> stringResource(R.string.xfer_verb_move_many, count)
        count == 1 -> stringResource(R.string.xfer_verb_copy_one)
        else -> stringResource(R.string.xfer_verb_copy_many, count)
    }
    val fromWord = stringResource(R.string.xfer_from_word)
    val toWord = stringResource(R.string.xfer_to_word)
    return buildAnnotatedString {
        append("$verb $fromWord ")
        withStyle(link) { append(from) }
        append(" $toWord ")
        withStyle(link) { append(to) }
    }
}

@Composable
private fun SmallIconButton(icon: FluentIcons, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val fill by com.droidexplorer.ui.design.pressedBackground(interaction)
    Box(
        Modifier.size(32.dp).clip(FluentShapes.control).background(fill)
            .semantics { contentDescription = label }
            .clickable(interactionSource = interaction, indication = null, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { FluentIcon(icon, tint = FluentTheme.colors.textPrimary, size = 14.dp) }
}

/** Speed history drawn like the Windows copy graph: grid on a pale background with the speed as a filled area. */
@Composable
private fun SpeedGraph(samples: List<Long>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(84.dp)) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFFFFFBE0))
        val grid = Color(0xFFE8DFA0)
        for (i in 1..4) drawLine(grid, androidx.compose.ui.geometry.Offset(0f, h * i / 5), androidx.compose.ui.geometry.Offset(w, h * i / 5), 1f)
        for (i in 1..8) drawLine(grid, androidx.compose.ui.geometry.Offset(w * i / 9, 0f), androidx.compose.ui.geometry.Offset(w * i / 9, h), 1f)
        if (samples.size >= 2) {
            val max = (samples.max() * 1.2f).coerceAtLeast(1f)
            val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
            val step = w / 59f
            val path = androidx.compose.ui.graphics.Path()
            fun x(i: Int): Float = if (rtl) w - i * step else i * step
            path.moveTo(x(0), h)
            samples.forEachIndexed { i, v -> path.lineTo(x(i), h - h * (v / max)) }
            path.lineTo(x(samples.size - 1), h)
            path.close()
            drawPath(path, Color(0xFFB8A500))
        }
        drawRect(Color(0xFFC9C28A), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}

/**
 * The File Explorer copy/move window: percent in the title, a heading with the folders as links,
 * the item name with percent, pause and cancel buttons, a progress bar (green while running, amber when paused)
 * and a "More details" expander with the speed graph, time and items remaining.
 */
@Composable
fun TransferProgressDialog(ui: TransferUi, paused: Boolean, onTogglePause: () -> Unit, onCancel: () -> Unit) {
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
    ExplorerDialog(title = stringResource(R.string.transfer_title, percent), onDismissRequest = onCancel) {
        Text(
            transferHeading(ui.moving, p.totalItems, ui.from, ui.to),
            style = FluentTheme.typography.body, color = c.textPrimary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.progress_heading, p.currentName.ifEmpty { ui.to }, percent),
                style = FluentTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                color = c.textPrimary, maxLines = 1, modifier = Modifier.weight(1f),
            )
            SmallIconButton(
                if (paused) FluentIcons.Play else FluentIcons.Pause,
                stringResource(if (paused) R.string.transfer_resume else R.string.transfer_pause),
                onTogglePause,
            )
            SmallIconButton(FluentIcons.Close, stringResource(R.string.dlg_cancel), onCancel)
        }
        FluentProgressBar(
            progress = percent / 100f,
            modifier = Modifier.testTag("transfer-bar"),
            color = if (paused) Color(0xFF9D5D00) else Color(0xFF0F7B0F),
        )
        if (more) {
            SpeedGraph(ui.speeds)
            Text(stringResource(R.string.transfer_name, p.currentName), style = text, color = c.textPrimary, maxLines = 1)
            Text(stringResource(R.string.transfer_time, time), style = text, color = c.textPrimary)
            Text(
                stringResource(R.string.transfer_remaining, (p.totalItems - p.doneItems).coerceAtLeast(0), formatBytes(remainingBytes)),
                style = text, color = c.textPrimary,
            )
        }
        Expander(more) { more = !more }
    }
}

/** The "More details / Fewer details" line with a divider above it. */
@Composable
private fun Expander(open: Boolean, onToggle: () -> Unit) {
    val c = FluentTheme.colors
    Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(c.divider))
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp).testTag("expander"),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (open) R.string.transfer_less else R.string.transfer_more),
            style = FluentTheme.typography.body, color = c.textSecondary,
        )
        Spacer(Modifier.width(8.dp))
        FluentIcon(if (open) FluentIcons.Up else FluentIcons.ChevronDown, tint = c.textSecondary, size = 10.dp)
    }
}

/** A large choice with a glyph; [highlight] draws the light-blue selected look Windows gives the first choice. */
@Composable
private fun CommandLink(text: String, icon: FluentIcons, tint: Color, highlight: Boolean = false, onClick: () -> Unit) {
    val c = FluentTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val fill by com.droidexplorer.ui.design.pressedBackground(
        interaction, if (highlight) c.accent.copy(alpha = if (c.isDark) 0.22f else 0.16f) else Color.Transparent,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FluentShapes.control)
            .background(fill, FluentShapes.control)
            .then(if (highlight) Modifier.border(1.dp, c.accent, FluentShapes.control) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = FluentTheme.typography.bodyLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
        FluentIcon(icon, tint = tint, size = 20.dp)
    }
}

/** What the person decided in the replace/skip/compare dialogs. */
data class ConflictAnswer(val choice: ConflictChoice, val applyToAll: Boolean, val skipIdentical: Boolean = false)

/**
 * "Replace or Skip Files": the heading with the folders as links, the file named in the destination, and the three
 * File Explorer choices (replace, skip, compare). The window has no buttons; its close button cancels.
 * "Compare info for both files" opens the "File Conflict" window: tick the version to keep, both keeps both.
 */
@Composable
fun ConflictDialogs(conflict: Conflict, totalItems: Int, moving: Boolean, onAnswer: (ConflictAnswer?) -> Unit) {
    var comparing by remember(conflict) { mutableStateOf(false) }
    var all by remember(conflict) { mutableStateOf(false) }
    var details by remember(conflict) { mutableStateOf(false) }
    val c = FluentTheme.colors
    val fromName = java.io.File(conflict.source.path).parentFile?.name.orEmpty()
    val toName = java.io.File(conflict.existing.path).parentFile?.name.orEmpty()
    val heading = transferHeading(moving, totalItems, fromName, toName)
    if (!comparing) {
        ExplorerDialog(title = stringResource(R.string.conflict_title), onDismissRequest = { onAnswer(null) }) {
            Text(heading, style = FluentTheme.typography.body, color = c.textPrimary)
            Text(
                stringResource(R.string.conflict_body, conflict.source.name),
                style = FluentTheme.typography.bodyLarge, color = c.textPrimary,
            )
            CommandLink(stringResource(R.string.conflict_replace), FluentIcons.Check, Color(0xFF107C10), highlight = true) {
                onAnswer(ConflictAnswer(ConflictChoice.REPLACE, all))
            }
            CommandLink(stringResource(R.string.conflict_skip), FluentIcons.Undo, c.accent) {
                onAnswer(ConflictAnswer(ConflictChoice.SKIP, all))
            }
            CommandLink(stringResource(R.string.conflict_compare), FluentIcons.Compare, c.accent) { comparing = true }
            if (totalItems > 1) {
                FluentCheckBox(checked = all, onCheckedChange = { all = it }, label = stringResource(R.string.conflict_all))
            }
            if (details) {
                val style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content)
                Text(stringResource(R.string.compare_from, fromName), style = FluentTheme.typography.bodyStrong, color = c.textPrimary)
                Text(stringResource(R.string.compare_size, formatBytes(conflict.source.sizeBytes)), style = style, color = c.textSecondary)
                Text(stringResource(R.string.compare_modified, ItemMapping.format(conflict.source.modifiedMillis)), style = style, color = c.textSecondary)
                Text(stringResource(R.string.compare_in, toName), style = FluentTheme.typography.bodyStrong, color = c.textPrimary)
                Text(stringResource(R.string.compare_size, formatBytes(conflict.existing.sizeBytes)), style = style, color = c.textSecondary)
                Text(stringResource(R.string.compare_modified, ItemMapping.format(conflict.existing.modifiedMillis)), style = style, color = c.textSecondary)
            }
            Expander(details) { details = !details }
        }
        return
    }
    var keepSource by remember(conflict) { mutableStateOf(false) }
    var keepExisting by remember(conflict) { mutableStateOf(false) }
    var skipIdentical by remember(conflict) { mutableStateOf(false) }
    ExplorerDialog(
        title = stringResource(R.string.compare_window),
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
                    onAnswer(ConflictAnswer(choice, false, skipIdentical))
                },
                isDefault = true,
                enabled = keepSource || keepExisting,
            )
            ExplorerDialogButton(stringResource(R.string.dlg_cancel), { onAnswer(null) })
        },
    ) {
        Text(stringResource(R.string.compare_title), style = FluentTheme.typography.titleLarge, color = c.accent)
        Text(stringResource(R.string.compare_body), style = FluentTheme.typography.body, color = c.textPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                FluentCheckBox(checked = keepSource, onCheckedChange = { keepSource = it }, label = stringResource(R.string.compare_from, fromName))
            }
            Column(Modifier.weight(1f)) {
                FluentCheckBox(checked = keepExisting, onCheckedChange = { keepExisting = it }, label = stringResource(R.string.compare_in, toName))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        Text(conflict.source.name, style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content), color = c.textSecondary, maxLines = 1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompareCell(conflict.source, keepSource, { keepSource = it }, Modifier.weight(1f))
            CompareCell(conflict.existing, keepExisting, { keepExisting = it }, Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
        FluentCheckBox(checked = skipIdentical, onCheckedChange = { skipIdentical = it }, label = stringResource(R.string.compare_skip_identical))
    }
}

/** One side of the comparison: glyph, date and size, and the tick box. */
@Composable
private fun CompareCell(item: StorageItem, checked: Boolean, onChecked: (Boolean) -> Unit, modifier: Modifier) {
    val c = FluentTheme.colors
    val style = FluentTheme.typography.body.copy(textDirection = TextDirection.Content)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        FileGlyph(ItemMapping.kindOf(item), glyphSize = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(ItemMapping.format(item.modifiedMillis), style = style, color = c.textPrimary)
            Text(formatBytes(item.sizeBytes), style = style, color = c.textPrimary)
        }
        FluentCheckBox(checked = checked, onCheckedChange = onChecked, label = null)
    }
}
