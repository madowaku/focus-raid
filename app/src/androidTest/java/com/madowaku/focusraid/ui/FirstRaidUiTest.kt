package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.madowaku.focusraid.core.model.SessionReward
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class FirstRaidUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val scenario = ReturnRaidScenario.demo(
        bossName = "環焔竜ヴォルガ",
        bossHp = 150,
        bossMaxHp = 250,
        playerDamage = 25,
        creditedMinutes = 25,
    )

    @Test
    fun returnEchoStrikeAndCampFlowIsReachable() {
        compose.setContent {
            FocusRaidTheme {
                ReturnRaidSequence(
                    scenario = scenario,
                    onFootprints = {},
                    onAgain = {},
                    onDone = {},
                )
            }
        }

        compose.onNodeWithText("帰還しました").assertIsDisplayed()

        compose.waitUntil(timeoutMillis = 7_000) {
            compose.onAllNodesWithTag("first_raid_strike")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("first_raid_strike")
            .assertIsDisplayed()
            .performClick()

        compose.waitUntil(timeoutMillis = 4_000) {
            compose.onAllNodesWithText("第001遠征隊")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("第001遠征隊").assertIsDisplayed()
        compose.onNodeWithText("最近の残響 3件 · 100分").assertIsDisplayed()
        compose.onNodeWithText("火をつなぎました").assertIsDisplayed()
        compose.onNodeWithText("👣  足跡を見る・残す").assertIsDisplayed()
        compose.onNodeWithText("もう25分").assertIsDisplayed()
        compose.onNodeWithText("今日はここまで").assertIsDisplayed()
    }

    @Test
    fun emptyEchoFeedNeverPretendsSomeoneElseWasThere() {
        val emptyScenario = ReturnRaidScenario.fromEchoes(
            bossName = "環焔竜ヴォルガ",
            bossHp = 181,
            bossMaxHp = 250,
            playerDamage = 25,
            creditedMinutes = 25,
            echoes = emptyList(),
            chainCountBefore = 0,
            chainMinutesBefore = 0,
        )

        compose.setContent {
            FocusRaidTheme {
                ReturnRaidSequence(
                    scenario = emptyScenario,
                    onFootprints = {},
                    onAgain = {},
                    onDone = {},
                )
            }
        }

        compose.waitUntil(timeoutMillis = 4_000) {
            compose.onAllNodesWithTag("first_raid_strike")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("まだ他の残響はありません").assertIsDisplayed()
        compose.onNodeWithTag("first_raid_strike").performClick()

        compose.waitUntil(timeoutMillis = 4_000) {
            compose.onAllNodesWithTag("first_raid_camp")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("あなたが最初の火を残しました").assertIsDisplayed()
        compose.onAllNodesWithText("誰かの集中").assertCountEquals(0)
    }

    @Test
    fun syncingRaidNeverTrapsCompletedFocusResult() {
        var skipped = false
        compose.setContent {
            FocusRaidTheme {
                FirstRaidEchoLoading(
                    state = FocusUiState(
                        reward = SessionReward(
                            creditedMinutes = 25,
                            personalDamage = 25,
                            worldEp = 25,
                            defeated = 1,
                            rarity = null,
                            discovery = null,
                            armoryPoints = 0,
                        ),
                    ),
                    onSkip = { skipped = true },
                )
            }
        }

        compose.onNodeWithText("帰還しました").assertIsDisplayed()
        compose.onNodeWithText("25分").assertIsDisplayed()
        compose.onNodeWithText("待たずに記録を見る").performClick()
        assertTrue(skipped)
    }
}
