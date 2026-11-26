package com.droidexplorer.feature.files

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidexplorer.storage.local.LocalStorageProvider
import com.droidexplorer.ui.design.DroidExplorerTheme
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Drives the real screen with real touch events: what a person taps must do what the button says. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-w1280dp-h800dp-xhdpi")
class LiveExplorerInteractionTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var root: File
    private lateinit var sd: File
    private var opened: String? = null

    @Before fun setUp() {
        root = Files.createTempDirectory("de-ui").toFile()
        sd = Files.createTempDirectory("de-sd").toFile()
        File(root, "Sub/inner").mkdirs()
        File(root, "Sub/note.txt").writeText("hello")
        File(sd, "card.txt").writeText("sd")
        rule.setContent {
            DroidExplorerTheme {
                LiveExplorerScreen(
                    provider = LocalStorageProvider(),
                    rootPath = root.path,
                    volumes = listOf(VolumeInfo("Internal", root.path), VolumeInfo("SD card", sd.path, true)),
                    onOpenFile = { opened = it },
                )
            }
        }
    }

    @After fun tearDown() { root.deleteRecursively(); sd.deleteRecursively() }

    private fun openSub() {
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Sub") > 0 }
        rule.onNodeWithText("Sub").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("note.txt") > 0 }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCount(t: String) =
        onAllNodesWithText(t).fetchSemanticsNodes().size

    @Test fun tappingAFileOpensIt() {
        openSub()
        rule.onNodeWithText("note.txt").performClick()
        rule.waitUntil(3000) { opened != null }
        assertEquals(File(root, "Sub/note.txt").path, opened)
    }

    @Test fun clickingAParentCrumbNavigatesThere() {
        openSub()
        rule.onNodeWithText("inner").performClick()
        rule.waitUntil(5000) { rule.onNodeWithTag("crumb-3").isDisplayedSafe() }
        // crumbs: This device > Internal > Sub > inner. Go back to "Sub".
        rule.onNodeWithTag("crumb-2").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("note.txt") > 0 }
    }

    @Test fun thisDeviceCrumbListsVolumesAndOpensTheSdCard() {
        openSub()
        rule.onNodeWithTag("crumb-0").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("SD card") > 1 }
        rule.onAllNodesWithText("SD card")[1].performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("card.txt") > 0 }
    }

    @Test fun copyThenPasteCreatesACopy() {
        openSub()
        rule.onNodeWithText("note.txt").performTouchInput { longClick() }
        rule.onNodeWithContentDescription("Copy").assertIsEnabled()
        rule.onNodeWithContentDescription("Copy").performClick()
        rule.onNodeWithContentDescription("Paste").assertIsEnabled()
        rule.onNodeWithContentDescription("Paste").performClick()
        rule.waitUntil(5000) { File(root, "Sub/note - עותק.txt").exists() }
        assertTrue(File(root, "Sub/note.txt").exists())
    }

    @Test fun editingThePathNavigates() {
        rule.onNodeWithTag("address-edit").performClick()
        rule.onNodeWithTag("address-input").performTextReplacement(File(root, "Sub").path)
        rule.onNodeWithTag("address-input").performImeAction()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("note.txt") > 0 }
    }

    @Test fun navigationTreeExpandsFoldersLikeWindows() {
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Sub") > 1 }
        assertEquals(0, rule.onAllNodesWithTextCount("inner"))
        rule.onNodeWithTag("nav-toggle-" + File(root, "Sub").path).performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("inner") > 0 }
        // Clicking a tree folder navigates to it.
        rule.onNodeWithText("inner").performClick()
        rule.waitUntil(5000) { rule.onNodeWithTag("crumb-3").isDisplayedSafe() }
    }

    @Test fun searchFiltersByNameInsideSubfolders() {
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Sub") > 0 }
        rule.onNodeWithTag("search-input").performTextReplacement("note")
        rule.waitUntil(8000) { rule.onAllNodesWithTextCount("note.txt") > 0 }
        assertEquals(0, rule.onAllNodesWithTextCount("inner"))
    }

    @Test fun viewMenuSwitchesToTilesAndStatusToggleBack() {
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Sub") > 0 }
        rule.onNodeWithText("View").performClick()
        rule.onNodeWithText("Tiles").performClick()
        rule.onNodeWithTag("view-tiles").assertExists()
        rule.onNodeWithText("View").performClick()
        rule.onNodeWithText("Details").performClick()
        rule.onNodeWithTag("view-details").assertExists()
    }

    @Test fun moreMenuSelectAllEnablesDelete() {
        openSub()
        rule.onNodeWithContentDescription("See more").performClick()
        rule.onNodeWithText("Select all").performClick()
        rule.onNodeWithContentDescription("Delete").assertIsEnabled()
    }

    @Test fun hiddenItemsAppearWhenToggled() {
        File(root, ".secret").writeText("x")
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Sub") > 0 }
        assertEquals(0, rule.onAllNodesWithTextCount(".secret"))
        rule.onNodeWithText("View").performClick()
        rule.onNodeWithText("Hidden items").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount(".secret") > 0 }
    }

    @Test fun conflictDialogReplaceOverwrites() {
        File(root, "a.txt").writeText("new")
        File(root, "Sub/a.txt").writeText("old")
        rule.onNodeWithContentDescription("Refresh").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("a.txt") > 0 }
        rule.onNodeWithText("a.txt").performTouchInput { longClick() }
        rule.onNodeWithContentDescription("Copy").performClick()
        rule.onNodeWithText("Sub").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("note.txt") > 0 }
        rule.onNodeWithContentDescription("Paste").performClick()
        rule.waitUntil(5000) { rule.onAllNodesWithTextCount("Replace the file in the destination") > 0 }
        rule.onNodeWithText("Replace the file in the destination").performClick()
        rule.waitUntil(5000) { File(root, "Sub/a.txt").readText() == "new" }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.isDisplayedSafe() =
        try { assertExists(); true } catch (_: AssertionError) { false }
}
