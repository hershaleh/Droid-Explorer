package com.droidexplorer.feature.files

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.droidexplorer.ui.design.FluentTheme
import com.droidexplorer.ui.design.controls.ExplorerDialog
import com.droidexplorer.ui.design.controls.ExplorerDialogButton
import com.droidexplorer.ui.design.controls.ExplorerTabs
import com.droidexplorer.ui.design.controls.FluentCheckBox

/**
 * Folder Options as in Windows, with the tabs that have something real to control today (View).
 * General and Search tabs are added when the features behind them exist (see docs/windows-explorer-dialogs.md).
 * OK and Apply write the settings; Cancel drops them; Restore Defaults resets the boxes.
 */
@Composable
fun FolderOptionsDialog(
    showHidden: Boolean,
    hideExtensions: Boolean,
    onApply: (showHidden: Boolean, hideExtensions: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var hidden by remember { mutableStateOf(showHidden) }
    var hideExt by remember { mutableStateOf(hideExtensions) }
    var appliedHidden by remember { mutableStateOf(showHidden) }
    var appliedExt by remember { mutableStateOf(hideExtensions) }
    val dirty = hidden != appliedHidden || hideExt != appliedExt
    fun apply() { onApply(hidden, hideExt); appliedHidden = hidden; appliedExt = hideExt }
    ExplorerDialog(
        title = stringResource(R.string.options_title),
        onDismissRequest = onClose,
        buttons = {
            ExplorerDialogButton(stringResource(R.string.dlg_ok), { apply(); onClose() }, isDefault = true)
            ExplorerDialogButton(stringResource(R.string.dlg_cancel), onClose)
            ExplorerDialogButton(stringResource(R.string.props_apply), { apply() }, enabled = dirty)
        },
    ) {
        ExplorerTabs(listOf(stringResource(R.string.options_view)), 0, {}) {
            Text(stringResource(R.string.options_advanced), style = FluentTheme.typography.body, color = FluentTheme.colors.textPrimary)
            FluentCheckBox(checked = hidden, onCheckedChange = { hidden = it }, label = stringResource(R.string.show_hidden))
            FluentCheckBox(checked = hideExt, onCheckedChange = { hideExt = it }, label = stringResource(R.string.options_hide_ext))
            ExplorerDialogButton(stringResource(R.string.options_restore), { hidden = false; hideExt = false })
        }
    }
}
