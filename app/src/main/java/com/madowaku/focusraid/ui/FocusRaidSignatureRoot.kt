package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.billing.ProAccessViewModel
import com.madowaku.focusraid.billing.PurchaseState
import com.madowaku.focusraid.core.domain.StarRoute
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase

/**
 * v0.7 product shell. It intentionally reuses persistence, billing, completion,
 * FIRST 25 and footprint behavior from the established root while swapping only
 * READY/Home and RUNNING/PAUSED presentation to the SIGNATURE surfaces.
 */
@Composable
fun FocusRaidSignatureRoot(
    viewModel: FocusViewModel,
    proAccessViewModel: ProAccessViewModel,
    systemAccess: FocusSystemAccess = FocusSystemAccess(),
    onRequestNotificationPermission: () -> Unit = {},
    onRequestExactAlarmPermission: () -> Unit = {},
    onPurchasePro: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proAccess by proAccessViewModel.access.collectAsStateWithLifecycle()
    val purchaseState by proAccessViewModel.purchaseState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showCustomDuration by rememberSaveable { mutableStateOf(false) }
    var customDurationMinutes by rememberSaveable { mutableStateOf(state.selectedMinutes) }
    var showEndConfirmation by rememberSaveable { mutableStateOf(false) }
    var showSystemAccessEducation by rememberSaveable { mutableStateOf(false) }
    var showFootprintDialog by rememberSaveable { mutableStateOf(false) }
    var showProPaywall by rememberSaveable { mutableStateOf(false) }
    var pendingProExpeditionName by rememberSaveable { mutableStateOf<String?>(null) }

    val openProPaywallFor: (Expedition?) -> Unit = { requestedExpedition ->
        pendingProExpeditionName = requestedExpedition?.name
        proAccessViewModel.clearPurchaseState()
        showProPaywall = true
        proAccessViewModel.refresh()
    }
    val openProPaywall = { openProPaywallFor(null) }

    BackHandler(
        enabled = !state.saving &&
            state.persistenceError == null &&
            (state.phase != SessionPhase.READY || tab != MainTab.HOME),
    ) {
        when (state.phase) {
            SessionPhase.RUNNING, SessionPhase.PAUSED -> showEndConfirmation = true
            SessionPhase.COMPLETED, SessionPhase.ABORTED -> {
                tab = MainTab.HOME
                viewModel.resetAfterResult()
            }
            SessionPhase.READY -> tab = MainTab.HOME
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase != SessionPhase.RUNNING && state.phase != SessionPhase.PAUSED) {
            showEndConfirmation = false
        }
        if (state.phase != SessionPhase.READY) {
            showCustomDuration = false
            showSystemAccessEducation = false
            showProPaywall = false
            pendingProExpeditionName = null
        }
        if (state.phase == SessionPhase.COMPLETED) {
            val reachedNewStarRouteCheckpoint = if (state.expedition == Expedition.STAR_ROUTE) {
                val creditedMinutes = state.reward?.creditedMinutes?.coerceAtLeast(0) ?: 0
                val beforeTotal = (state.totalFocusMinutes - creditedMinutes).coerceAtLeast(0)
                StarRoute.reachedCheckpoint(state.totalFocusMinutes) >
                    StarRoute.reachedCheckpoint(beforeTotal)
            } else {
                true
            }
            showFootprintDialog = reachedNewStarRouteCheckpoint
        } else {
            showFootprintDialog = false
        }
    }

    LaunchedEffect(proAccess.accessLevel, purchaseState) {
        if (proAccess.accessLevel == AccessLevel.PRO && purchaseState == PurchaseState.Success) {
            pendingProExpeditionName
                ?.let { name -> runCatching { Expedition.valueOf(name) }.getOrNull() }
                ?.takeIf { expedition -> FeatureAccess.canUse(expedition, AccessLevel.PRO) }
                ?.let { expedition ->
                    tab = MainTab.HOME
                    viewModel.selectExpedition(expedition)
                }
            pendingProExpeditionName = null
            showProPaywall = false
            proAccessViewModel.clearPurchaseState()
        }
    }

    CompositionLocalProvider(
        LocalCompanionIdentity provides state.companion,
        LocalProAccessLevel provides proAccess.accessLevel,
        LocalOpenProPaywall provides openProPaywall,
        LocalRetryWorld provides viewModel::retryWorldContributions,
    ) {
        Box {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    when {
                        initialState == SessionPhase.READY && targetState == SessionPhase.RUNNING -> {
                            (fadeIn(animationSpec = tween(durationMillis = 620, delayMillis = 70)) +
                                scaleIn(animationSpec = tween(durationMillis = 620), initialScale = .94f)) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 220)) +
                                    scaleOut(animationSpec = tween(durationMillis = 280), targetScale = 1.03f))
                        }

                        targetState == SessionPhase.READY -> {
                            (fadeIn(animationSpec = tween(durationMillis = 420)) +
                                scaleIn(animationSpec = tween(durationMillis = 420), initialScale = .98f)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 220))
                        }

                        else -> fadeIn(animationSpec = tween(durationMillis = 320)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 220))
                    }
                },
                label = "focus-raid-signature-phase",
            ) { animatedPhase ->
                FocusRaidSignatureContent(
                    state = state.copy(phase = animatedPhase),
                    tab = tab,
                    onTabChange = { tab = it },
                    onSelectCompanion = viewModel::selectCompanion,
                    onSelectMinutes = viewModel::selectMinutes,
                    onSelectExpedition = { expedition ->
                        if (FeatureAccess.canUse(expedition, proAccess.accessLevel)) {
                            viewModel.selectExpedition(expedition)
                        } else {
                            openProPaywallFor(expedition)
                        }
                    },
                    onTimerClick = {
                        customDurationMinutes = state.selectedMinutes
                        showCustomDuration = true
                    },
                    onStart = {
                        if (!FeatureAccess.canUse(state.expedition, proAccess.accessLevel)) {
                            openProPaywallFor(state.expedition)
                        } else if (!systemAccess.isReady && !state.systemAccessEducationSeen) {
                            showSystemAccessEducation = true
                        } else {
                            viewModel.start()
                        }
                    },
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onFinishEarly = { showEndConfirmation = true },
                    onAgain = {
                        if (FeatureAccess.canUse(state.expedition, proAccess.accessLevel)) {
                            viewModel.startAgain()
                        } else {
                            viewModel.resetAfterResult()
                            openProPaywallFor(state.expedition)
                        }
                    },
                    onDone = viewModel::confirmResultAndReset,
                )
            }

            if (state.phase == SessionPhase.COMPLETED) {
                KenneyCompletionOverlay(triggerKey = state.resultSessionId)
            }
        }

        if (showProPaywall && state.phase == SessionPhase.READY) {
            ProPaywallDialog(
                access = proAccess,
                purchaseState = purchaseState,
                onPurchase = onPurchasePro,
                onRestore = onRestorePurchases,
                onRetry = { proAccessViewModel.refresh() },
                onDismiss = {
                    pendingProExpeditionName = null
                    proAccessViewModel.clearPurchaseState()
                    showProPaywall = false
                },
            )
        }
    }

    if (state.persistenceError != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("記録を保存できません") },
            text = { Text(state.persistenceError.orEmpty()) },
            confirmButton = {
                Button(
                    onClick = viewModel::retryPersistence,
                    enabled = !state.saving,
                ) {
                    Text("再試行")
                }
            },
        )
    }

    if (showCustomDuration && state.phase == SessionPhase.READY) {
        CustomDurationSheet(
            minutes = customDurationMinutes,
            onMinutesChange = { customDurationMinutes = it },
            onConfirm = {
                viewModel.selectMinutes(customDurationMinutes)
                showCustomDuration = false
            },
            onDismiss = { showCustomDuration = false },
        )
    }

    if (showSystemAccessEducation && state.phase == SessionPhase.READY) {
        FocusSystemAccessDialog(
            access = systemAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestExactAlarmPermission = onRequestExactAlarmPermission,
            onDismiss = { showSystemAccessEducation = false },
            onContinue = {
                viewModel.markSystemAccessEducationSeen()
                showSystemAccessEducation = false
                if (FeatureAccess.canUse(state.expedition, proAccess.accessLevel)) {
                    viewModel.start()
                } else {
                    openProPaywallFor(state.expedition)
                }
            },
        )
    }

    if (
        showEndConfirmation &&
        (state.phase == SessionPhase.RUNNING || state.phase == SessionPhase.PAUSED)
    ) {
        SessionExitConfirmDialog(
            state = state,
            onDismiss = { showEndConfirmation = false },
            onConfirm = {
                showEndConfirmation = false
                viewModel.finishEarly()
            },
        )
    }

    if (showFootprintDialog && state.phase == SessionPhase.COMPLETED) {
        FootprintDialog(
            state = state,
            onSelectPreset = viewModel::selectFootprintPreset,
            onLeaveFootprint = viewModel::leaveFootprint,
            onRetry = viewModel::retryFootprints,
            onDismiss = { showFootprintDialog = false },
        )
    }
}
