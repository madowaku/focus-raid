package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FirstRunUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = FocusUiState(
        initialized = true,
        phase = SessionPhase.READY,
        totalFocusMinutes = 0,
        world = WorldSnapshot(
            bossName = "環焔竜ヴォルガ",
            bossHp = 428_192,
            bossMaxHp = 650_000,
        ),
    )

    @Test
    fun act_two_large_text_keeps_next_and_skip_reachable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FocusRaidTheme {
                    FirstRunMoment(state = state, initialAct = 1, onSkip = {}, onComplete = {})
                }
            }
        }
        compose.onNodeWithTag("first_run_skip").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("first_run_primary").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("3/3").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fresh_user_reads_all_three_acts_in_order() {
        var completed = false
        compose.setContent {
            FocusRaidTheme {
                FirstRunMoment(state = state, onSkip = {}, onComplete = { completed = true })
            }
        }

        compose.onNodeWithText("1/3").assertIsDisplayed()
        compose.onNodeWithText("25分が、一撃になる。").assertIsDisplayed()
        compose.onNodeWithText("集中を完走すると、その時間がRaidへ届きます。").assertIsDisplayed()

        compose.onNodeWithTag("first_run_primary").performClick()
        compose.onNodeWithText("2/3").assertIsDisplayed()
        compose.onNodeWithText("世界中の集中が、\n同じボスへ。").assertIsDisplayed()
        compose.onNodeWithText("あなたの25分も、誰かの25分も。\nみんなで、このレイドボスを倒します。").assertIsDisplayed()

        compose.onNodeWithTag("first_run_primary").performClick()
        compose.onNodeWithText("3/3").assertIsDisplayed()
        compose.onNodeWithText("相棒も、一緒に育つ。").assertIsDisplayed()
        compose.onNodeWithText("最初の集中へ").assertIsDisplayed()
        compose.onNodeWithText("あとで見る").assertDoesNotExist()
        compose.onNodeWithText("最初の集中へ").performClick()
        assertTrue(completed)
    }

    @Test
    fun act_two_delivers_three_lights_without_fake_activity_copy() {
        val delivered = mutableListOf<Int>()
        compose.setContent {
            FocusRaidTheme {
                FirstRunMoment(
                    state = state,
                    initialAct = 1,
                    onSkip = {},
                    onComplete = {},
                    onOtherLight = { delivered += it },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 4_000) { delivered.size == 3 }
        compose.onNodeWithText("世界中の集中が、\n同じボスへ。").assertIsDisplayed()
        compose.onNodeWithText("fake").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(0, 1, 2), delivered) }
    }

    @Test
    fun skip_remains_available_before_final_act() {
        var skipped = false
        compose.setContent {
            FocusRaidTheme {
                FirstRunMoment(
                    state = state,
                    initialAct = 0,
                    onSkip = { skipped = true },
                    onComplete = {},
                )
            }
        }

        compose.onNodeWithText("あとで見る").performClick()
        assertTrue(skipped)
    }

    @Test
    fun final_act_uses_primary_as_only_exit() {
        var completed = false
        compose.setContent {
            FocusRaidTheme {
                FirstRunMoment(
                    state = state,
                    initialAct = 2,
                    onSkip = {},
                    onComplete = { completed = true },
                )
            }
        }

        compose.onNodeWithText("最初の集中へ").assertIsDisplayed()
        compose.onNodeWithText("あとで見る").assertDoesNotExist()
        compose.onNodeWithTag("first_run_primary").performClick()
        assertTrue(completed)
    }
}
