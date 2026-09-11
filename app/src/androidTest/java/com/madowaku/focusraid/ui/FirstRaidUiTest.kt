package com.madowaku.focusraid.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import org.junit.Rule
import org.junit.Test

class FirstRaidUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val scenario = ReturnRaidScenario.demo(
        bossName = "環焔竜ヴォルガ",
        bossHp = 620,
        bossMaxHp = 1_000,
        playerDamage = 100,
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
        compose.onNodeWithText("火をつなぎました").assertIsDisplayed()
        compose.onNodeWithText("👣  足跡を見る・残す").assertIsDisplayed()
        compose.onNodeWithText("もう25分").assertIsDisplayed()
        compose.onNodeWithText("今日はここまで").assertIsDisplayed()
    }
}
