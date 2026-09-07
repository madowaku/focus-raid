package com.madowaku.focusraid

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.madowaku.focusraid.billing.*
import com.madowaku.focusraid.core.model.*
import com.madowaku.focusraid.ui.*
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class ProductAccessibilityTest {
    @get:Rule val compose = createComposeRule()
    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FocusRaidTheme { content() }
            }
        }
    }
    @Test fun maximumTimerRemainsOneLineWithAccessibleDuration() {
        show { FocusRaidAppContent(FocusUiState(selectedMinutes = 180), onTimerClick = {}) }
        compose.onNodeWithContentDescription("集中タイマー").assertIsDisplayed()
            .assert(hasStateDescription("180分0秒"))
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("180:00", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Timer text must fit its ring: size=${layouts.single().size}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}", layouts.single().hasVisualOverflow)
        compose.onNodeWithText("⚔  レイド開始").performScrollTo().assertIsDisplayed()
    }
    @Test fun pausedTimerAndExitRemainReachable() {
        show { FocusRaidAppContent(FocusUiState(phase = SessionPhase.PAUSED)) }
        compose.onNodeWithContentDescription("集中タイマー").assertIsDisplayed()
        compose.onNodeWithText("▶  再開する").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("セッションを終了").performScrollTo().assertIsDisplayed()
    }
    @Test fun footprintPostingNeverDisablesDismiss() {
        var dismissed = false
        show { FootprintDialog(FocusUiState(phase = SessionPhase.COMPLETED, footprintPosting = true), {}, {}, onDismiss = { dismissed = true }) }
        compose.onNodeWithText("今は残さない").assertIsEnabled().performClick()
        assertTrue(dismissed)
    }
    @Test fun billingErrorOffersRetryRestoreAndFreeExit() {
        var retried = false; var dismissed = false
        show { ProPaywallDialog(ProAccessState(errorMessage = DefaultProAccessRepository.REFRESH_ERROR),
            PurchaseState.Error(DefaultProAccessRepository.ACTION_ERROR), {}, {}, { dismissed = true }, { retried = true }) }
        compose.onNodeWithText("商品情報を再読み込み").assertIsDisplayed().performClick()
        assertTrue(retried)
        compose.onNodeWithText("購入を復元").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Freeで続ける").assertIsDisplayed().performClick()
        assertTrue(dismissed)
    }
    @Test fun customDurationControlsDescribeTheirAction() {
        show { CustomDurationSheet(180, {}, {}, {}) }
        compose.onNodeWithContentDescription("1分減らす").assertIsEnabled()
        compose.onNodeWithContentDescription("1分増やす").assertIsNotEnabled()
        compose.onNodeWithText("180 分に設定").performScrollTo().assertIsDisplayed()
    }
}
