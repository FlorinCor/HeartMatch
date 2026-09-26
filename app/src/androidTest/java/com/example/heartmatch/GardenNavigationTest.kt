package com.example.heartmatch

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises the home route without claiming rewards or making puzzle moves. */
class GardenNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 20_000) {
            runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }

    @Test fun launchOpensGardenAndSettingsReturnsHome() {
        compose.onNodeWithText("Your garden journey").assertIsDisplayed()
        compose.onNodeWithText("PLAY NOW ♥").assertDoesNotExist()
        compose.onNodeWithContentDescription("Settings and help").performClick()
        compose.onNodeWithText("Reduce decoration motion").assertIsDisplayed()
        compose.onNodeWithText("How to play").performClick()
        compose.onNodeWithText("Basics").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Your garden journey").assertIsDisplayed()
    }

    @Test fun playAndPauseReturnToGarden() {
        compose.onNode(hasText("Play level", substring=true) and hasClickAction()).performClick()
        val board = compose.onNodeWithTag("puzzle-board").fetchSemanticsNode().boundsInRoot
        val hud = compose.onNodeWithTag("puzzle-hud").fetchSemanticsNode().boundsInRoot
        val boosters = compose.onNodeWithTag("puzzle-boosters").fetchSemanticsNode().boundsInRoot
        assertTrue("Board must clear the HUD", board.top >= hud.bottom)
        assertTrue("Board must clear the boosters", board.bottom <= boosters.top)
        compose.onNodeWithContentDescription("Pause").performClick()
        awaitText("Resume")
        compose.onNodeWithText("Resume").assertIsDisplayed()
        compose.onNodeWithText("Back to garden").performClick()
        compose.onNodeWithText("Your garden journey").assertIsDisplayed()
    }

    @Test fun shopOpensAndReturnsToGarden() {
        compose.onNode(hasText("· Shop", substring=true)).performClick()
        compose.onNodeWithText("Garden shop").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Your garden journey").assertIsDisplayed()
    }

    @Test fun dailyGiftRemainsAccessibleWithoutClaiming() {
        compose.onNodeWithContentDescription("Daily gift").performClick()
        compose.onNodeWithText("DAILY REWARD").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Your garden journey").assertIsDisplayed()
    }
}
