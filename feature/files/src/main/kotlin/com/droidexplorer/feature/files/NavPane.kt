package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

@Composable
private fun NavTreeItem(
    label: String,
    icon: @Composable () -> Unit,
    depth: Int = 0,
    expanded: Boolean? = null,
    selected: Boolean = false,
) {
    val c = FluentTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .height(32.dp)
            .clip(FluentShapes.control)
            .background(if (selected) c.subtleFillSecondary else Color.Transparent, FluentShapes.control),
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(16.dp)
                    .background(c.accent, RoundedCornerShape(2.dp)),
            )
        }
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = (8 + depth * 16).dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                if (expanded != null) {
                    FluentIcon(
                        if (expanded) FluentIcons.ChevronDown else FluentIcons.ChevronRight,
                        tint = c.textSecondary,
                        size = 10.dp,
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = FluentTheme.typography.body,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NavDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(1.dp)
            .background(FluentTheme.colors.controlStrokeSecondary),
    )
}

/** Navigation pane on the Mica background: Home, Quick access tree, This device, Cloud and Network. */
@Composable
fun ExplorerNavPane(modifier: Modifier = Modifier) {
    val c = FluentTheme.colors
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(c.windowBackground)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        NavTreeItem(stringResource(R.string.nav_home), { FluentIcon(FluentIcons.Home) })
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_quick_access), { FluentIcon(FluentIcons.Home, tint = c.accent) }, expanded = true)
        NavTreeItem(stringResource(R.string.nav_downloads), { FluentIcon(FluentIcons.Download) }, depth = 1, selected = true)
        NavTreeItem(stringResource(R.string.nav_documents), { FluentIcon(FluentIcons.Document) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_pictures), { FluentIcon(FluentIcons.Picture) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_music), { FluentIcon(FluentIcons.Music) }, depth = 1)
        NavTreeItem(stringResource(R.string.nav_videos), { FluentIcon(FluentIcons.Video) }, depth = 1)
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_this_device), { FluentIcon(FluentIcons.Network) }, expanded = true)
        NavTreeItem(stringResource(R.string.nav_internal), { FluentIcon(FluentIcons.Drive) }, depth = 1, expanded = false)
        NavTreeItem(stringResource(R.string.nav_sd), { FluentIcon(FluentIcons.Drive) }, depth = 1, expanded = false)
        NavDivider()
        NavTreeItem(stringResource(R.string.nav_cloud), { FluentIcon(FluentIcons.Cloud) }, expanded = false)
        NavTreeItem(stringResource(R.string.nav_network), { FluentIcon(FluentIcons.Network) }, expanded = false)
    }
}
