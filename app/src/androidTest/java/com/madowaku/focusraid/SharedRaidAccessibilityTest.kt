package com.madowaku.focusraid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.ui.FocusUiState
import com.madowaku.focusraid.ui.PixelExpeditionDestinationScreen
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SharedRaidAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    @Test fun pausedControlsRemainReachableAtLargeFontOnSmallScreen() {
        var resumed = false
        var finished = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FocusRaidTheme {
                    Box(Modifier.requiredSize(360.dp, 640.dp)) {
                        PixelExpeditionDestinationScreen(
                            FocusUiState(phase = SessionPhase.PAUSED, selectedMinutes = 180, remainingSeconds = 10_800),
                            onPause = {}, onResume = { resumed = true }, onRequestFinish = { finished = true })
                    }
                }
            }
        }
        compose.onNodeWithText("集中を再開").assertIsDisplayed().performClick()
        compose.onNodeWithText("セッションを終了").assertIsDisplayed().performClick()
        assertTrue(resumed)
        assertTrue(finished)
        compose.onNodeWithContentDescription("残り時間").performScrollTo().assertIsDisplayed()
    }
}
