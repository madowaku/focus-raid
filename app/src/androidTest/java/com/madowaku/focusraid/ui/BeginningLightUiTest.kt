package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
        compose.setContent {
            FocusRaidTheme {
                BeginningLightMoment(
                    totalFocusMinutes = 25,
                    creditedMinutes = 25,
                    companion = CompanionIdentity.RAG,
                    onAgain = {},
                    onDone = {},
                )
            }
        }

        compose.onNodeWithText("はじまりの灯").assertIsDisplayed()
        compose.onNodeWithText("25 / 75分").assertIsDisplayed()
        compose.onNodeWithTag("beginning_light_egg").performClick()
        compose.onNodeWithText("…こつん。").assertIsDisplayed()
        compose.onNodeWithText("もう25分").assertIsDisplayed()
        compose.onNodeWithText("今日はここまで").assertIsDisplayed()
    }
}
