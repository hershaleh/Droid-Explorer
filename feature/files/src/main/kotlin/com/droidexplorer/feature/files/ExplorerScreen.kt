package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentTheme

/**
 * File Explorer shell modelled on Windows 11 File Explorer: tab strip, toolbar with address bar and search,
 * command bar, navigation pane (wide screens), details list and status bar.
 * Width classes: Wide >= 840dp (all columns + nav pane), Medium >= 600dp, Narrow below.
 */
@Composable
fun ExplorerScreen(
    modifier: Modifier = Modifier,
    items: List<ExplorerItem> = SampleData.downloads,
    actions: ExplorerActions = ExplorerActions(),
    crumbs: List<String>? = null,
    title: String? = null,
    grouped: Boolean = true,
    emptyText: String? = null,
    interactive: Boolean = true,
) {
    val c = FluentTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(c.windowBackground)) {
        val layout = when {
            maxWidth >= 840.dp -> ExplorerLayout.Wide
            maxWidth >= 600.dp -> ExplorerLayout.Medium
            else -> ExplorerLayout.Narrow
        }
        Column(Modifier.fillMaxSize()) {
            ExplorerTabStrip(titles = listOf(title ?: stringResource(R.string.nav_downloads)), selectedIndex = 0)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(c.layer),
            ) {
                ExplorerToolbar(showSearchBox = layout != ExplorerLayout.Narrow, actions = actions, crumbs = crumbs)
                ExplorerCommandBar(
                    hasSelection = items.any { it.selected },
                    compact = layout == ExplorerLayout.Narrow,
                    actions = actions,
                    singleSelection = items.count { it.selected } == 1,
                    interactive = interactive,
                )
                Row(Modifier.weight(1f)) {
                    if (layout == ExplorerLayout.Wide) ExplorerNavPane()
                    ExplorerDetailsList(items, layout, Modifier.weight(1f), actions, grouped, emptyText)
                }
                ExplorerStatusBar(items)
            }
        }
    }
}
