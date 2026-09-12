package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Rule
import org.junit.Test

class BeginningLightUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun firstTwentyFiveMomentRespondsAndKeepsNextActionsVisible() {
        var knocks = 0
        compose.setContent {
            FocusRaidTheme {
                BeginningLightMoment(
                    totalFocusMinutes = 25,
                    creditedMinutes = 25,
                    companion = CompanionIdentity.RAG,
                    onAgain = {},
                    onDone = {},
                    onKnock = { knocks++ },
                )
            }
        }

        compose.onNodeWithText("はじまりの灯").assertIsDisplayed()
        compose.onNodeWithText("25 / 75分").assertIsDisplayed()
        compose.onNodeWithTag("beginning_light_egg").performClick()
        compose.onNodeWithText("…こつん。").assertIsDisplayed()
        compose.runOnIdle { org.junit.Assert.assertEquals(1, knocks) }
        compose.onNodeWithText("もう25分").assertIsDisplayed()
        compose.onNodeWithText("今日はここまで").assertIsDisplayed()
    }

    @Test
    fun firstLongSessionShowsHatchedCompanionInsteadOfContradictingEgg() {
        compose.setContent {
            FocusRaidTheme {
                BeginningLightMoment(
                    totalFocusMinutes = 90,
                    creditedMinutes = 90,
                    companion = CompanionIdentity.RAG,
                    onAgain = {},
                    onDone = {},
                )
            }
        }

        compose.onNodeWithTag("beginning_light_hatched").assertIsDisplayed()
        compose.onNodeWithText("灯の向こうで、相棒が目を開いた。").assertIsDisplayed()
        compose.onNodeWithText("75 / 75分").assertIsDisplayed()
        compose.onNodeWithText("孵化しました。最初の灯は相棒画面に残ります。").assertIsDisplayed()
        compose.onAllNodesWithText("卵に触れてみる").assertCountEquals(0)
        compose.onNodeWithText("もう25分").assertIsDisplayed()
        compose.onNodeWithText("今日はここまで").assertIsDisplayed()
    }
}
