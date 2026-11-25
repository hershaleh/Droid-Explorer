package com.droidexplorer.ui.design.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

/**
 * Windows 11 ContentDialog: 8dp corners, lighter content area over a darker command area,
 * title in Subtitle style, and two equal-width buttons (primary = accent, first in reading order).
 */
@Composable
fun FluentContentDialog(
    title: String,
    onDismissRequest: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: () -> Unit = onDismissRequest,
    content: @Composable () -> Unit,
) {
    val c = FluentTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .padding(24.dp)
                .widthIn(min = 320.dp, max = 548.dp)
                .clip(FluentShapes.overlay)
                .border(1.dp, c.surfaceStroke, FluentShapes.overlay),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.windowBackgroundTertiary)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = FluentTheme.typography.subtitle, color = c.textPrimary)
                content()
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.windowBackground)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FluentButton(onClick = onPrimary, style = ButtonStyle.Accent, modifier = Modifier.weight(1f)) {
                    Text(primaryText)
                }
                if (secondaryText != null) {
                    FluentButton(onClick = onSecondary, modifier = Modifier.weight(1f)) {
                        Text(secondaryText)
                    }
                }
            }
        }
    }
}
