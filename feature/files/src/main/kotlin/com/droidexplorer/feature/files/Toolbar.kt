package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ButtonStyle
import com.droidexplorer.ui.design.controls.FluentButton
import com.droidexplorer.ui.design.controls.FluentTextBox
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun ToolbarIconButton(icon: FluentIcons, enabled: Boolean = true) {
    FluentButton(
        onClick = {},
        style = ButtonStyle.Subtle,
        enabled = enabled,
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.size(32.dp),
    ) {
        FluentIcon(icon, size = 16.dp)
    }
}

/** Back, forward, up and refresh buttons, the breadcrumb address bar and the search box. */
@Composable
fun ExplorerToolbar(showSearchBox: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToolbarIconButton(FluentIcons.Back)
        ToolbarIconButton(FluentIcons.Forward, enabled = false)
        ToolbarIconButton(FluentIcons.Up)
        ToolbarIconButton(FluentIcons.Refresh)
        Spacer(Modifier.width(4.dp))
        AddressBar(Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        if (showSearchBox) {
            var query by remember { mutableStateOf("") }
            FluentTextBox(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier.width(280.dp),
                trailing = { FluentIcon(FluentIcons.Search, tint = FluentTheme.colors.textSecondary) },
            )
        } else {
            ToolbarIconButton(FluentIcons.Search)
        }
    }
}

/** Windows 11 address bar: folder glyph, breadcrumb segments separated by chevrons, history chevron. */
@Composable
fun AddressBar(modifier: Modifier = Modifier) {
    val c = FluentTheme.colors
    val segments = listOf(
        stringResource(R.string.addr_this_device),
        stringResource(R.string.addr_internal),
        stringResource(R.string.nav_downloads),
    )
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(FluentShapes.control)
            .background(c.controlFillInput, FluentShapes.control)
            .border(1.dp, c.controlStroke, FluentShapes.control)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderGlyph()
        Spacer(Modifier.width(4.dp))
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEachIndexed { i, label ->
                if (i > 0) FluentIcon(FluentIcons.ChevronRight, tint = c.textSecondary, size = 10.dp)
                Box(Modifier.clip(FluentShapes.control).padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(label, style = FluentTheme.typography.body, color = c.textPrimary, maxLines = 1)
                }
            }
        }
        FluentIcon(FluentIcons.ChevronDown, tint = c.textSecondary, size = 10.dp)
    }
}

@Composable
private fun CommandDivider() {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(20.dp)
            .background(FluentTheme.colors.controlStrokeSecondary),
    )
}

@Composable
private fun CommandIcon(icon: FluentIcons, enabled: Boolean = true) {
    ToolbarIconButton(icon, enabled)
}

@Composable
private fun CommandLabeled(icon: FluentIcons, label: String, showLabel: Boolean, chevron: Boolean) {
    FluentButton(
        onClick = {},
        style = ButtonStyle.Subtle,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
    ) {
        FluentIcon(icon, size = 16.dp)
        if (showLabel) Text(label, style = FluentTheme.typography.body)
        if (chevron) FluentIcon(FluentIcons.ChevronDown, size = 10.dp)
    }
}

/**
 * Windows 11 command bar: "New" with label, icon-only Cut/Copy/Paste/Rename/Share/Delete,
 * then Sort and View, then "See more". Selection-dependent commands are disabled with no selection.
 */
@Composable
fun ExplorerCommandBar(hasSelection: Boolean, compact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CommandLabeled(FluentIcons.Add, stringResource(R.string.cmd_new), showLabel = true, chevron = true)
        CommandDivider()
        CommandIcon(FluentIcons.Cut, hasSelection)
        CommandIcon(FluentIcons.Copy, hasSelection)
        CommandIcon(FluentIcons.Paste, enabled = false)
        CommandIcon(FluentIcons.Rename, hasSelection)
        CommandIcon(FluentIcons.Share, hasSelection)
        CommandIcon(FluentIcons.Delete, hasSelection)
        CommandDivider()
        CommandLabeled(FluentIcons.Sort, stringResource(R.string.cmd_sort), showLabel = !compact, chevron = true)
        CommandLabeled(FluentIcons.View, stringResource(R.string.cmd_view), showLabel = !compact, chevron = true)
        CommandDivider()
        CommandIcon(FluentIcons.More)
    }
}
