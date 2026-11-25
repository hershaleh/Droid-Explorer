package com.droidexplorer.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.droidexplorer.ui.design.controls.ButtonStyle
import com.droidexplorer.ui.design.controls.FluentButton
import com.droidexplorer.ui.design.controls.FluentCheckBox
import com.droidexplorer.ui.design.controls.FluentContentDialog
import com.droidexplorer.ui.design.controls.FluentInfoBar
import com.droidexplorer.ui.design.controls.FluentNavigationItem
import com.droidexplorer.ui.design.controls.FluentProgressBar
import com.droidexplorer.ui.design.controls.FluentProgressRing
import com.droidexplorer.ui.design.controls.FluentTextBox
import com.droidexplorer.ui.design.controls.FluentToggleSwitch
import com.droidexplorer.ui.design.controls.InfoBarSeverity
import com.droidexplorer.ui.design.controls.SettingsCard

/** Component gallery used to compare Droid Explorer controls with their Windows 11 counterparts. */
@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    val c = FluentTheme.colors
    val t = FluentTheme.typography
    var text by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(true) }
    var toggled by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(0) }
    var dialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.windowBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Droid Explorer", style = t.title, color = c.textPrimary)
        Text("גלריית רכיבים – Windows 11", style = t.subtitle, color = c.textPrimary)

        Text("Buttons", style = t.bodyStrong, color = c.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FluentButton(onClick = {}, style = ButtonStyle.Accent) { Text("Accent") }
            FluentButton(onClick = {}) { Text("Standard") }
            FluentButton(onClick = {}, style = ButtonStyle.Subtle) { Text("Subtle") }
            FluentButton(onClick = {}, enabled = false) { Text("Disabled") }
        }

        Text("Text box", style = t.bodyStrong, color = c.textPrimary)
        FluentTextBox(value = text, onValueChange = { text = it }, placeholder = "חיפוש / Search", modifier = Modifier.fillMaxWidth())

        Text("Toggle and check box", style = t.bodyStrong, color = c.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            FluentToggleSwitch(checked = toggled, onCheckedChange = { toggled = it })
            FluentCheckBox(checked = checked, onCheckedChange = { checked = it }, label = "Check box")
        }

        Text("Progress", style = t.bodyStrong, color = c.textPrimary)
        FluentProgressBar(progress = 0.6f)
        FluentProgressBar(progress = null)
        FluentProgressRing()

        Text("Navigation", style = t.bodyStrong, color = c.textPrimary)
        Column(Modifier.fillMaxWidth()) {
            FluentNavigationItem(selected == 0, { selected = 0 }, "בית", icon = { Icon(Icons.Filled.Home, null, tint = c.textPrimary) })
            FluentNavigationItem(selected == 1, { selected = 1 }, "הגדרות", icon = { Icon(Icons.Filled.Settings, null, tint = c.textPrimary) })
        }

        Text("Settings cards", style = t.bodyStrong, color = c.textPrimary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsCard(
                title = "מצב כהה",
                description = "החלפה בין ערכת נושא בהירה לכהה",
                icon = { Icon(Icons.Filled.Info, null, tint = c.textPrimary) },
                trailing = { FluentToggleSwitch(checked = toggled, onCheckedChange = { toggled = it }) },
            )
            SettingsCard(title = "Language", description = "Hebrew (default) / English")
        }

        Text("Info bars", style = t.bodyStrong, color = c.textPrimary)
        FluentInfoBar("Information", "Message text.", severity = InfoBarSeverity.Informational)
        FluentInfoBar("Success", "Operation completed.", severity = InfoBarSeverity.Success)
        FluentInfoBar("Warning", "This action cannot be undone.", severity = InfoBarSeverity.Warning)
        FluentInfoBar("Error", "Something went wrong.", severity = InfoBarSeverity.Error)

        FluentButton(onClick = { dialog = true }) { Text("Show dialog") }
    }

    if (dialog) {
        FluentContentDialog(
            title = "מחיקת קובץ?",
            onDismissRequest = { dialog = false },
            primaryText = "מחק",
            onPrimary = { dialog = false },
            secondaryText = "ביטול",
        ) {
            Text("הקובץ יועבר לסל המחזור.", style = t.body, color = c.textPrimary)
        }
    }
}
