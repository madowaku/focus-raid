package com.madowaku.focusraid.walking

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WalkingUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun permissionIsOnlyRequestedAfterExplanationAndCancelIsHarmless() {
        var requests = 0
        compose.setContent {
            FocusRaidTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    WalkingPanel(WalkingActions(WalkingUiState(status = WalkingStatus.DISABLED), { requests++ }, {}, {}, {}))
                }
            }
        }
        compose.onNodeWithText("歩数を使う").performClick()
        compose.onNodeWithText("今は使わない").assertIsDisplayed().performClick()
        assertEquals(0, requests)
        compose.onNodeWithText("歩数を使う").performClick()
        // Dialog window semantics scope selects the visible confirmation.
        compose.onNodeWithText("日常の歩行を、小さな冒険に").assertIsDisplayed()
        compose.onNodeWithTag("walking-consent-confirm").performClick()
        assertEquals(1, requests)
    }
}
