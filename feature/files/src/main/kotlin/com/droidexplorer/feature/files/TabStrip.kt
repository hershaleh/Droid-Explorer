package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ButtonStyle
import com.droidexplorer.ui.design.controls.FluentButton
import com.droidexplorer.ui.design.icons.FluentIcon
import com.droidexplorer.ui.design.icons.FluentIcons

/**
 * Explorer tab strip: tabs sit on the window background (Mica) and the selected tab shares the
 * fill of the layer below it, so it appears connected to the toolbar.
 */
@Composable
fun ExplorerTabStrip(
    titles: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val c = FluentTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .height(36.dp)
                    .background(
                        if (selected) c.layer else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    )
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FolderGlyph()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = FluentTheme.typography.body,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                FluentIcon(FluentIcons.Close, tint = c.textSecondary, size = 12.dp)
            }
        }
        FluentButton(
            onClick = {},
            style = ButtonStyle.Subtle,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
            modifier = Modifier.size(32.dp).padding(bottom = 0.dp),
        ) {
            FluentIcon(FluentIcons.Add, size = 14.dp)
        }
    }
}

/** Convenience for previews and tests. */
@Composable
fun defaultTabTitle(): String = stringResource(R.string.nav_downloads)
