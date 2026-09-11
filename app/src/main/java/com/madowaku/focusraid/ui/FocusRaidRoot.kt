package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.billing.ProAccessViewModel
import com.madowaku.focusraid.billing.PurchaseState
import com.madowaku.focusraid.core.domain.StarRoute
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.SessionPhase

@Composable
fun FocusRaidRoot(
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

    BackHandler(enabled = !state.saving && state.persistenceError == null && (state.phase != SessionPhase.READY || tab != MainTab.HOME)) {
        when (state.phase) {
            SessionPhase.RUNNING, SessionPhase.PAUSED -> showEndConfirmation = true
            SessionPhase.COMPLETED, SessionPhase.ABORTED -> { tab = MainTab.HOME; viewModel.resetAfterResult() }
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
        if (
            proAccess.accessLevel == AccessLevel.PRO &&
            purchaseState == PurchaseState.Success
        ) {
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
                                scaleIn(animationSpec = tween(durationMillis = 620), initialScale = 0.94f)) togetherWith
                                (fadeOut(animationSpec = tween(durationMillis = 220)) +
                                    scaleOut(animationSpec = tween(durationMillis = 280), targetScale = 1.03f))
                        }

                        targetState == SessionPhase.READY -> {
                            (fadeIn(animationSpec = tween(durationMillis = 420)) +
                                scaleIn(animationSpec = tween(durationMillis = 420), initialScale = 0.98f)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 220))
                        }

                        else -> {
                            fadeIn(animationSpec = tween(durationMillis = 320)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 220))
                        }
                    }
                },
                label = "focus-raid-root-phase",
            ) { animatedPhase ->
                FocusRaidAppContent(
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
                        if (FeatureAccess.canUse(state.expedition, proAccess.accessLevel)) viewModel.startAgain()
                        else { viewModel.resetAfterResult(); openProPaywallFor(state.expedition) }
                    },
                    onDone = viewModel::resetAfterResult,
                )
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
            onDismissRequest = {}, title = { Text("記録を保存できません") },
            text = { Text(state.persistenceError.orEmpty()) },
            confirmButton = { Button(onClick = viewModel::retryPersistence, enabled = !state.saving) { Text("再試行") } },
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
                if (FeatureAccess.canUse(state.expedition, proAccess.accessLevel)) viewModel.start()
                else openProPaywallFor(state.expedition)
            },
        )
    }

    if (showEndConfirmation &&
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

@Composable
internal fun FootprintDialog(
    state: FocusUiState,
    onSelectPreset: (String) -> Unit,
    onLeaveFootprint: () -> Unit,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val location = when (state.expedition) {
        Expedition.TOWER -> "天空塔 ${state.world.towerFloor}F"
        Expedition.ABYSS -> "深層迷宮 ${state.world.abyssDepth}m"
        Expedition.STAR_ROUTE -> "星渡り航路 第${StarRoute.reachedCheckpoint(state.totalFocusMinutes)}星標"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("この場所の足跡")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    location,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                if (state.worldSyncStatus == com.madowaku.focusraid.data.WorldSyncStatus.LOCAL_PREVIEW) {
                    Text("ローカル体験用の足跡です。他のプレイヤーには送信されません。", style = MaterialTheme.typography.bodySmall)
                }
                when {
                    state.footprintLoadError != null -> {
                        Text(state.footprintLoadError, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("再読み込み") }
                    }
                    state.footprintsLoading -> {
                        Text(
                            "この場所の足跡を探しています…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.footprints.isEmpty() -> {
                        Text(
                            "まだ足跡はありません。最初のひとことを残せます。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        state.footprints.take(3).forEach { footprint ->
                            Text(
                                "${footprint.glyph}  ${footprint.text}  ·  ${footprint.relativeLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (state.footprintPosted) {
                    Text(
                        "✓ あなたの足跡を残しました",
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        if (state.footprintPosting) {
                            "足跡を届けています…"
                        } else {
                            "あなたも定型メッセージを1つ残せます"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.footprintPostError?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FootprintPresets.all.take(6).chunked(1).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = state.selectedFootprintPresetId == preset.id,
                                    onClick = { onSelectPreset(preset.id) },
                                    enabled = !state.footprintPosting,
                                    label = {
                                        Text(
                                            "${preset.glyph} ${preset.text}",
                                            maxLines = 2,
                                        )
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.footprintPosted) {
                Button(onClick = onDismiss) {
                    Text("閉じる")
                }
            } else {
                Button(
                    onClick = onLeaveFootprint,
                    enabled = state.selectedFootprintPresetId != null && !state.footprintPosting,
                ) {
                    Text(if (state.footprintPosting) "送信中…" else "足跡を残す")
                }
            }
        },
        dismissButton = {
            if (!state.footprintPosted) {
                TextButton(onClick = onDismiss) {
                    Text("今は残さない")
                }
            }
        },
    )
}

@Composable
internal fun FocusSystemAccessDialog(
    access: FocusSystemAccess,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("終了通知を確実に届ける")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "画面を閉じていても集中終了を知らせるため、通知と正確なアラームを有効にできます。許可しなくてもタイマーは使えます。",
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (access.notificationsEnabled) "✓ 通知：有効" else "通知：設定が必要",
                    color = if (access.notificationsEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (!access.notificationsEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("通知を許可")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (access.exactAlarmsEnabled) "✓ 正確な終了時刻：有効" else "正確な終了時刻：設定が必要",
                    color = if (access.exactAlarmsEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (!access.exactAlarmsEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRequestExactAlarmPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("正確な終了時刻を有効にする")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "設定しない場合も終了時刻は端末内に保存され、アプリへ戻ったとき正しい残り時間へ復元します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(if (access.isReady) "レイド開始" else "このまま開始")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        },
    )
}

@Composable
internal fun SessionExitConfirmDialog(
    state: FocusUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val elapsedSeconds = (state.durationSeconds - state.remainingSeconds).coerceAtLeast(0)
    val elapsedMinutes = elapsedSeconds / 60
    val progressCopy = if (elapsedMinutes > 0) {
        "ここまでの集中 ${elapsedMinutes}分は戦果として残ります。"
    } else {
        "まだ1分未満なので、今回は戦果の加算はありません。"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("セッションを終了しますか？")
        },
        text = {
            Text("$progressCopy\n完走ボーナスはつきません。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "終了する",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("集中を続ける")
            }
        },
    )
}
