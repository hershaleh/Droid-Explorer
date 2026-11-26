package com.droidexplorer.feature.files

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidexplorer.ui.design.DroidExplorerTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the Explorer shell to PNG (only when the Roborazzi record task runs). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExplorerScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String, dark: Boolean, rtl: Boolean) {
        rule.setContent {
            val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                DroidExplorerTheme(darkTheme = dark) { ExplorerScreen() }
            }
        }
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    @Config(sdk = [34], qualifiers = "w1280dp-h800dp-xhdpi")
    fun tabletLightRtlHebrew() = shot("explorer-tablet-light-rtl", dark = false, rtl = true)

    @Test
    @Config(sdk = [34], qualifiers = "en-w1280dp-h800dp-xhdpi")
    fun tabletDarkLtrEnglish() = shot("explorer-tablet-dark-ltr", dark = true, rtl = false)

    @Test
    @Config(sdk = [34], qualifiers = "w412dp-h892dp-xxhdpi")
    fun phoneLightRtlHebrew() = shot("explorer-phone-light-rtl", dark = false, rtl = true)

    @Test
    @Config(sdk = [34], qualifiers = "en-w412dp-h892dp-xxhdpi")
    fun phoneDarkLtrEnglish() = shot("explorer-phone-dark-ltr", dark = true, rtl = false)
}
