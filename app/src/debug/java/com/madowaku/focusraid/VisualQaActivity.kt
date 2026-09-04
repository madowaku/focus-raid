package com.madowaku.focusraid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import com.madowaku.focusraid.ui.CustomDurationSheet
import com.madowaku.focusraid.ui.FocusRaidAppContent
import com.madowaku.focusraid.ui.FocusSystemAccess
import com.madowaku.focusraid.ui.FocusSystemAccessDialog
import com.madowaku.focusraid.ui.FocusUiState
import com.madowaku.focusraid.ui.MainTab
import com.madowaku.focusraid.ui.SessionExitConfirmDialog
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

class VisualQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val phase = intent.getStringExtra(EXTRA_PHASE)?.uppercase().orEmpty()
        val state = when (phase) {
            "RAID" -> FocusUiState(
                phase = SessionPhase.RUNNING,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 18 * 60 + 42,
            )

            "PAUSED" -> FocusUiState(
                phase = SessionPhase.PAUSED,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 18 * 60 + 42,
            )

            "END_CONFIRM" -> FocusUiState(
                phase = SessionPhase.RUNNING,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 13 * 60,
            )

            "ABORTED" -> FocusUiState(
                phase = SessionPhase.ABORTED,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 0,
                reward = SessionReward(
                    creditedMinutes = 12,
                    personalDamage = 12,
                    worldEp = 12,
                    defeated = 0,
                    rarity = null,
                    discovery = null,
                    armoryPoints = 0,
                ),
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
                if (phase == "CUSTOM") {
                    CustomDurationSheet(
                        minutes = 30,
                        onMinutesChange = {},
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
                if (phase == "SYSTEM_ACCESS") {
                    FocusSystemAccessDialog(
                        access = FocusSystemAccess(
                            notificationsEnabled = false,
                            exactAlarmsEnabled = false,
                        ),
                        onRequestNotificationPermission = {},
                        onRequestExactAlarmPermission = {},
                        onDismiss = {},
                        onContinue = {},
                    )
                }
                if (phase == "END_CONFIRM") {
                    SessionExitConfirmDialog(
                        state = state,
                        onDismiss = {},
                        onConfirm = {},
                    )
                }
            }
        }
    }

    private companion object {
        const val EXTRA_PHASE = "phase"
    }
}
