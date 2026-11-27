package com.droidexplorer.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.FluentShapes
import com.droidexplorer.ui.design.FluentTheme

/**
 * The in-place name editor of Windows File Explorer: the name is selected when it opens (without the
 * extension for files), Enter or leaving the field confirms, Escape cancels.
 */
@Composable
fun InlineRenameField(
    initial: String,
    isFolder: Boolean,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val c = FluentTheme.colors
    val dot = initial.lastIndexOf('.')
    val end = if (isFolder || dot <= 0) initial.length else dot
    var value by remember(initial) { mutableStateOf(TextFieldValue(initial, TextRange(0, end))) }
    val focus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    fun commit() {
        if (finished) return
        finished = true
        onCommit(value.text)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(c.accent, c.accent.copy(alpha = 0.55f)),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = singleLine,
            textStyle = FluentTheme.typography.body.copy(color = c.textPrimary),
            cursorBrush = SolidColor(c.textPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = modifier
                .background(c.controlFillInputActive, FluentShapes.control)
                .border(1.dp, c.accent, FluentShapes.control)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .focusRequester(focus)
                .onFocusChanged { st ->
                    if (st.isFocused) hadFocus = true else if (hadFocus) commit()
                }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        finished = true
                        onCancel()
                        true
                    } else {
                        false
                    }
                }
                .testTag("rename-input"),
        )
    }
}
