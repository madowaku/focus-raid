package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Rule
import org.junit.Test

class PixelExpeditionFocusingUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun ridge_state_reads_as_world_progress_not_only_timer() {
        compose.setContent {
            FocusRaidTheme {
                PixelExpeditionFocusingScreen(
                    state = sampleState(remainingSeconds = 12 * 60 + 30),
                    progressOverride = .50f,
                    onPause = {},
                    onResume = {},
                    onRequestFinish = {},
                )
            }
        }

        compose.onNodeWithText("12:30").assertIsDisplayed()
        compose.onNodeWithText("もう山道まで来た").assertIsDisplayed()
        compose.onNodeWithText("RIDGE · 山道").assertIsDisplayed()
        compose.onNodeWithText("Ⅱ  一時停止").assertIsDisplayed()
    }

    @Test
    fun last_minute_surfaces_raid_approach() {
        compose.setContent {
            FocusRaidTheme {
                PixelExpeditionFocusingScreen(
                    state = sampleState(remainingSeconds = 59),
                    progressOverride = .96f,
                    onPause = {},
                    onResume = {},
                    onRequestFinish = {},
                )
            }
        }

        compose.onNodeWithText("00:59").assertIsDisplayed()
        compose.onNodeWithText("レイド地点の灯が見えてきた").assertIsDisplayed()
        compose.onNodeWithText("RAID · 到着間近").assertIsDisplayed()
    }

    @Test
    fun paused_state_keeps_world_meaning_and_resume_action() {
        compose.setContent {
            FocusRaidTheme {
                PixelExpeditionFocusingScreen(
                    state = sampleState(
                        remainingSeconds = 12 * 60 + 30,
                        phase = SessionPhase.PAUSED,
                    ),
                    progressOverride = .50f,
                    onPause = {},
                    onResume = {},
                    onRequestFinish = {},
                )
            }
        }

        compose.onNodeWithText("旅はここで止まっています").assertIsDisplayed()
        compose.onNodeWithText("一時停止中").assertIsDisplayed()
        compose.onNodeWithText("▶  集中を再開").assertIsDisplayed()
    }

    private fun sampleState(
        remainingSeconds: Int,
        phase: SessionPhase = SessionPhase.RUNNING,
    ) = FocusUiState(
        initialized = true,
        phase = phase,
        selectedMinutes = 25,
        remainingSeconds = remainingSeconds,
        durationSeconds = 25 * 60,
        expedition = Expedition.ABYSS,
        world = WorldSnapshot(
            bossName = "環焔竜ヴォルガ",
            bossHp = 428_192,
            bossMaxHp = 650_000,
        ),
    )
}
