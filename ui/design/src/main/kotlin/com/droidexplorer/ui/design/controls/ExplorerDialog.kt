package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

/**
 * The classic Windows File Explorer dialog (copy progress, replace or skip, delete confirmation, errors):
 * a small title bar with a close button, a flat content area, and a grey command strip with
 * right-aligned standard buttons. Settings-style dialogs use [FluentContentDialog] instead.
 */
@Composable
fun ExplorerDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = FluentTheme.colors
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = modifier
                .padding(16.dp)
                .widthIn(min = 300.dp, max = 480.dp)
                .clip(FluentShapes.overlay)
                .border(1.dp, c.surfaceStroke, FluentShapes.overlay),
        ) {
            Row(
                Modifier.fillMaxWidth().background(c.windowBackground).padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = FluentTheme.typography.caption,
                    color = c.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(width = 44.dp, height = 32.dp).clickable(onClick = onDismissRequest),
                    contentAlignment = Alignment.Center,
                ) { FluentIcon(FluentIcons.Close, tint = c.textPrimary, size = 10.dp) }
            }
            Column(
                Modifier.fillMaxWidth().background(c.flyoutFill).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
            if (buttons != null) {
                Row(
                    Modifier.fillMaxWidth().background(c.windowBackground).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) { buttons() }
            }
        }
    }
}

/** Standard dialog button; the default one carries an accent outline like the Windows default button. */
@Composable
fun ExplorerDialogButton(text: String, onClick: () -> Unit, isDefault: Boolean = false, enabled: Boolean = true) {
    val c = FluentTheme.colors
    FluentButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .defaultMinSize(minWidth = 88.dp)
            .then(if (isDefault && enabled) Modifier.border(2.dp, c.accent, FluentShapes.control) else Modifier),
    ) { Text(text) }
}

/** Blue heading line, as used by the copy and replace dialogs. */
@Composable
fun ExplorerDialogHeading(text: String) {
    Text(text, style = FluentTheme.typography.bodyLarge, color = FluentTheme.colors.accent)
}

/**
 * Classic property-sheet tabs: a row of tabs over a bordered panel, the selected tab joined to the panel.
 * Used by the Properties and Folder Options windows.
 */
@Composable
fun ExplorerTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = FluentTheme.colors
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (on) c.flyoutFill else c.windowBackground)
                        .border(1.dp, c.surfaceStroke, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .clickable { onSelect(i) }
                        .padding(horizontal = 14.dp, vertical = if (on) 8.dp else 5.dp),
                ) {
                    Text(label, style = FluentTheme.typography.body, color = c.textPrimary, maxLines = 1)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().border(1.dp, c.surfaceStroke).background(c.flyoutFill).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
