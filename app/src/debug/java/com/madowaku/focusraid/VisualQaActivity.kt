package com.madowaku.focusraid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
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
        val now = System.currentTimeMillis()
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

            "LOG" -> FocusUiState(
                totalFocusMinutes = 682,
                sessionHistory = listOf(
                    SessionHistoryEntry(
                        sessionId = "visual-log-1",
                        completedAtEpochMillis = now - 18 * 60 * 1000L,
                        plannedMinutes = 25,
                        creditedMinutes = 25,
                        expedition = Expedition.TOWER,
                        outcome = SessionOutcome.COMPLETED,
                        damage = 25,
                        rarity = Rarity.RARE,
                        discovery = "古代の鍵",
                    ),
                    SessionHistoryEntry(
                        sessionId = "visual-log-2",
                        completedAtEpochMillis = now - 2 * 60 * 60 * 1000L,
                        plannedMinutes = 45,
                        creditedMinutes = 45,
                        expedition = Expedition.ABYSS,
                        outcome = SessionOutcome.COMPLETED,
                        damage = 45,
                        rarity = null,
                        discovery = null,
                    ),
                    SessionHistoryEntry(
                        sessionId = "visual-log-3",
                        completedAtEpochMillis = now - 24 * 60 * 60 * 1000L,
                        plannedMinutes = 25,
                        creditedMinutes = 12,
                        expedition = Expedition.TOWER,
                        outcome = SessionOutcome.ABORTED,
                        damage = 12,
                        rarity = null,
                        discovery = null,
                    ),
                ),
            )

            else -> FocusUiState()
        }

        setContent {
            FocusRaidTheme {
                FocusRaidAppContent(
                    state = state,
                    tab = if (phase == "LOG") MainTab.LOG else MainTab.HOME,
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
