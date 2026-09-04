package com.madowaku.focusraid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import com.madowaku.focusraid.ui.FocusRaidAppContent
import com.madowaku.focusraid.ui.FocusUiState
import com.madowaku.focusraid.ui.MainTab
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

class VisualQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val phase = intent.getStringExtra(EXTRA_PHASE)?.uppercase().orEmpty()
        val state = when (phase) {
            "RAID" -> FocusUiState(
                phase = SessionPhase.RUNNING,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 18 * 60 + 42,
            )

            "VICTORY" -> FocusUiState(
                phase = SessionPhase.COMPLETED,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 0,
                reward = SessionReward(
                    creditedMinutes = 25,
                    personalDamage = 25,
                    worldEp = 25,
                    defeated = 1,
                    rarity = Rarity.RARE,
                    discovery = "古代の鍵",
                    armoryPoints = 2,
                ),
            )

            else -> FocusUiState()
        }

        setContent {
            FocusRaidTheme {
                FocusRaidAppContent(
                    state = state,
                    tab = MainTab.HOME,
                )
            }
        }
    }

    private companion object {
        const val EXTRA_PHASE = "phase"
    }
}
