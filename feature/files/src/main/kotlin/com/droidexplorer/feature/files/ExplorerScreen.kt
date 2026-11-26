package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
    emptyText: String? = null,
    interactive: Boolean = true,
    nav: NavPaneModel? = null,
    revealPath: String? = null,
    onRevealed: () -> Unit = {},
) {
    val c = FluentTheme.colors
    var navOpen by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize().background(c.windowBackground)) {
        val layout = when {
            maxWidth >= 840.dp -> ExplorerLayout.Wide
            maxWidth >= 600.dp -> ExplorerLayout.Medium
            else -> ExplorerLayout.Narrow
        }
        val toolbarActions = if (nav != null && layout != ExplorerLayout.Wide) actions.copy(onMenu = { navOpen = true }) else actions
        Column(Modifier.fillMaxSize()) {
            ExplorerTabStrip(titles = listOf(title ?: stringResource(R.string.nav_downloads)), selectedIndex = 0)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(c.layer),
            ) {
                ExplorerToolbar(showSearchBox = layout != ExplorerLayout.Narrow, actions = toolbarActions, crumbs = crumbs)
                ExplorerCommandBar(
                    hasSelection = items.any { it.selected },
                    compact = layout == ExplorerLayout.Narrow,
                    actions = actions,
                    singleSelection = items.count { it.selected } == 1,
                    interactive = interactive,
                )
                Row(Modifier.weight(1f)) {
                    if (layout == ExplorerLayout.Wide) ExplorerNavPane(model = nav)
                    if (actions.viewMode == ViewMode.Details) {
                        ExplorerDetailsList(items, layout, Modifier.weight(1f), actions, emptyText, revealPath, onRevealed)
                    } else {
                        ExplorerGridList(items, actions.viewMode, Modifier.weight(1f), actions, emptyText, revealPath, onRevealed)
                    }
                }
                ExplorerStatusBar(items, actions.viewMode, actions.onViewMode)
            }
        }
        if (navOpen && nav != null && layout != ExplorerLayout.Wide) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { navOpen = false },
            )
            ExplorerNavPane(
                modifier = Modifier.align(Alignment.CenterStart),
                model = nav.copy(onNavigate = { navOpen = false; nav.onNavigate(it) }),
            )
        }
    }
}
