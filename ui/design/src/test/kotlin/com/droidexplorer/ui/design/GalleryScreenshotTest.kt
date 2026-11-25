package com.droidexplorer.ui.design

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the component gallery to PNG files so the look can be reviewed without a device.
 * Files are written only when the Roborazzi record task runs (recordRoborazziDebug).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h2000dp-xxhdpi")
class GalleryScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String, dark: Boolean, rtl: Boolean) {
        rule.setContent {
            val direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                DroidExplorerTheme(darkTheme = dark) { GalleryScreen() }
            }
        }
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun lightRtl() = shot("gallery-light-rtl", dark = false, rtl = true)

    @Test fun darkRtl() = shot("gallery-dark-rtl", dark = true, rtl = true)

    @Test fun lightLtr() = shot("gallery-light-ltr", dark = false, rtl = false)
}
