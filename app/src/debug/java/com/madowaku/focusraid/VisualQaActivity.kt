package com.madowaku.focusraid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.billing.ProAccessState
import com.madowaku.focusraid.billing.ProProduct
import com.madowaku.focusraid.billing.PurchaseState
import com.madowaku.focusraid.core.domain.CompanionEvolution
import com.madowaku.focusraid.core.domain.CompanionStage
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
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
import com.madowaku.focusraid.ui.FootprintDialog
import com.madowaku.focusraid.ui.LocalProAccessLevel
import com.madowaku.focusraid.ui.MainTab
import com.madowaku.focusraid.ui.ProPaywallDialog
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
        val visualHistory = listOf(
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
                sessionId = "visual-log-star",
                completedAtEpochMillis = now - 5 * 60 * 60 * 1000L,
                plannedMinutes = 25,
                creditedMinutes = 25,
                expedition = Expedition.STAR_ROUTE,
                outcome = SessionOutcome.COMPLETED,
                damage = 25,
                rarity = Rarity.RARE,
                discovery = "彗星のコンパス",
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
        )
        val visualFootprints = listOf(
            Footprint(
                expedition = Expedition.TOWER,
                checkpoint = 4_281,
                presetId = "waiting",
                glyph = "⌁",
                text = "先で待ってる！",
                relativeLabel = "18分前",
            ),
            Footprint(
                expedition = Expedition.TOWER,
                checkpoint = 4_281,
                presetId = "one_step",
                glyph = "◆",
                text = "今日も一歩！",
                relativeLabel = "2時間前",
            ),
            Footprint(
                expedition = Expedition.TOWER,
                checkpoint = 4_281,
                presetId = "rest",
                glyph = "☕",
                text = "休憩も大事",
                relativeLabel = "昨日",
            ),
        )
        val footprintBase = FocusUiState(
            phase = SessionPhase.COMPLETED,
            selectedMinutes = 25,
            durationSeconds = 25 * 60,
            remainingSeconds = 0,
            totalFocusMinutes = 707,
            footprints = visualFootprints,
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

        val state = when (phase) {
            "RAID" -> FocusUiState(
                phase = SessionPhase.RUNNING,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 18 * 60 + 42,
            )

            "RAID_OVERVIEW", "RAID_OVERVIEW_PRO" -> FocusUiState(
                totalFocusMinutes = 682,
                todayFocusMinutes = 70,
                streakDays = 4,
                sessionHistory = visualHistory,
            )

            "STAR_READY" -> FocusUiState(
                selectedMinutes = 25,
                expedition = Expedition.STAR_ROUTE,
                totalFocusMinutes = 50,
                streakDays = 4,
            )

            "STAR_ROUTE" -> FocusUiState(
                phase = SessionPhase.RUNNING,
                selectedMinutes = 25,
                expedition = Expedition.STAR_ROUTE,
                durationSeconds = 25 * 60,
                remainingSeconds = 18 * 60 + 42,
                totalFocusMinutes = 50,
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

            "VICTORY" -> footprintBase.copy(footprints = emptyList())

            "FOOTPRINT_LOADING" -> footprintBase.copy(
                footprints = emptyList(),
                footprintsLoading = true,
            )

            "FOOTPRINT_PRESENT" -> footprintBase

            "FOOTPRINT_POSTING" -> footprintBase.copy(
                selectedFootprintPresetId = "keep_going",
                footprintPosting = true,
            )

            "FOOTPRINT_ERROR" -> footprintBase.copy(
                selectedFootprintPresetId = "keep_going",
                footprintPostError = "足跡を送信できませんでした。通信を確認してもう一度お試しください。",
            )

            "FOOTPRINT_POSTED" -> footprintBase.copy(
                footprints = listOf(
                    Footprint(
                        expedition = Expedition.TOWER,
                        checkpoint = 4_281,
                        presetId = "keep_going",
                        glyph = "✦",
                        text = "がんばろう！",
                        relativeLabel = "たった今",
                    ),
                ) + visualFootprints.take(2),
                selectedFootprintPresetId = "keep_going",
                footprintPosted = true,
            )

            "STAR_VICTORY" -> FocusUiState(
                phase = SessionPhase.COMPLETED,
                selectedMinutes = 25,
                expedition = Expedition.STAR_ROUTE,
                durationSeconds = 25 * 60,
                remainingSeconds = 0,
                totalFocusMinutes = 75,
                reward = SessionReward(
                    creditedMinutes = 25,
                    personalDamage = 25,
                    worldEp = 25,
                    defeated = 1,
                    rarity = Rarity.RARE,
                    discovery = "彗星のコンパス",
                    armoryPoints = 2,
                ),
            )

            "STAR_CONTINUE" -> FocusUiState(
                phase = SessionPhase.COMPLETED,
                selectedMinutes = 15,
                expedition = Expedition.STAR_ROUTE,
                durationSeconds = 15 * 60,
                remainingSeconds = 0,
                totalFocusMinutes = 15,
                reward = SessionReward(
                    creditedMinutes = 15,
                    personalDamage = 15,
                    worldEp = 15,
                    defeated = 0,
                    rarity = null,
                    discovery = null,
                    armoryPoints = 0,
                ),
            )

            "EVOLUTION" -> FocusUiState(
                phase = SessionPhase.COMPLETED,
                selectedMinutes = 25,
                durationSeconds = 25 * 60,
                remainingSeconds = 0,
                totalFocusMinutes = 720,
                companionEvolution = CompanionEvolution(
                    from = CompanionStage.HATCHLING,
                    to = CompanionStage.FIRST_GROWTH,
                ),
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

            "COMPANION_EGG" -> FocusUiState(
                totalFocusMinutes = 0,
                todayFocusMinutes = 0,
                streakDays = 0,
            )

            "COMPANION" -> FocusUiState(
                totalFocusMinutes = 645,
                todayFocusMinutes = 50,
                streakDays = 4,
            )

            "COMPANION_FIRST" -> FocusUiState(
                totalFocusMinutes = 1_200,
                todayFocusMinutes = 45,
                streakDays = 7,
            )

            "COMPANION_SECOND" -> FocusUiState(
                totalFocusMinutes = 3_000,
                todayFocusMinutes = 60,
                streakDays = 12,
            )

            "COMPANION_MATURE" -> FocusUiState(
                totalFocusMinutes = 5_000,
                todayFocusMinutes = 75,
                streakDays = 21,
            )

            "LOG", "LOG_PRO" -> FocusUiState(
                totalFocusMinutes = 682,
                todayFocusMinutes = 70,
                streakDays = 4,
                sessionHistory = visualHistory,
            )

            else -> FocusUiState()
        }

        val tab = when {
            phase.startsWith("COMPANION") -> MainTab.COMPANION
            phase.startsWith("RAID_OVERVIEW") -> MainTab.RAID
            phase.startsWith("LOG") -> MainTab.LOG
            else -> MainTab.HOME
        }
        val accessLevel = if (phase.startsWith("STAR") || phase.endsWith("_PRO")) {
            AccessLevel.PRO
        } else {
            AccessLevel.FREE
        }

        setContent {
            FocusRaidTheme {
                CompositionLocalProvider(LocalProAccessLevel provides accessLevel) {
                    FocusRaidAppContent(
                        state = state,
                        tab = tab,
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
                    if (phase == "PAYWALL") {
                        ProPaywallDialog(
                            access = ProAccessState(
                                accessLevel = AccessLevel.FREE,
                                product = ProProduct(
                                    productId = "focus_raid_pro_lifetime",
                                    formattedPrice = "¥XXX",
                                ),
                            ),
                            purchaseState = PurchaseState.Idle,
                            onPurchase = {},
                            onRestore = {},
                            onDismiss = {},
                        )
                    }
                    if (phase.startsWith("FOOTPRINT_")) {
                        FootprintDialog(
                            state = state,
                            onSelectPreset = {},
                            onLeaveFootprint = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val EXTRA_PHASE = "phase"
    }
}
