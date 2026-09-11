package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.ProAccessViewModel
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.SessionPhase

@Composable
fun FocusRaidV06Root(
    viewModel: FocusViewModel,
    proAccessViewModel: ProAccessViewModel,
    systemAccess: FocusSystemAccess = FocusSystemAccess(),
    onRequestNotificationPermission: () -> Unit = {},
    onRequestExactAlarmPermission: () -> Unit = {},
    onPurchasePro: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reward = state.reward
    val first25Reserved = reward?.let {
        reservesFirst25Moment(
            totalFocusMinutes = state.totalFocusMinutes,
            creditedMinutes = it.creditedMinutes,
        )
    } ?: false
    val showFirstRaid = state.phase == SessionPhase.COMPLETED &&
        reward != null &&
        reward.creditedMinutes > 0 &&
        !first25Reserved

    if (!showFirstRaid) {
        FocusRaidRoot(
            viewModel = viewModel,
            proAccessViewModel = proAccessViewModel,
            systemAccess = systemAccess,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestExactAlarmPermission = onRequestExactAlarmPermission,
            onPurchasePro = onPurchasePro,
            onRestorePurchases = onRestorePurchases,
        )
        return
    }

    var showFootprints by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }
    val scenario = ReturnRaidScenario.demo(
        bossName = state.world.bossName,
        bossHp = state.world.bossHp,
        bossMaxHp = state.world.bossMaxHp,
        playerDamage = reward.personalDamage,
        creditedMinutes = reward.creditedMinutes,
    )

    BackHandler(enabled = !state.saving) {
        viewModel.resetAfterResult()
    }

    ReturnRaidSequence(
        scenario = scenario,
        onFootprints = { showFootprints = true },
        onAgain = viewModel::startAgain,
        onDone = viewModel::resetAfterResult,
    )

    if (showFootprints) {
        FirstRaidFootprintDialog(
            state = state,
            onSelectPreset = viewModel::selectFootprintPreset,
            onLeaveFootprint = viewModel::leaveFootprint,
            onRetry = viewModel::retryFootprints,
            onDismiss = { showFootprints = false },
        )
    }
}

@Composable
private fun FirstRaidFootprintDialog(
    state: FocusUiState,
    onSelectPreset: (String) -> Unit,
    onLeaveFootprint: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val location = when (state.expedition) {
        Expedition.TOWER -> "天空塔 ${state.world.towerFloor}F"
        Expedition.ABYSS -> "深層迷宮 ${state.world.abyssDepth}m"
        Expedition.STAR_ROUTE -> "星路のキャンプ"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("この場所の足跡") },
        text = {
            Column {
                Text(
                    location,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))

                when {
                    state.footprintsLoading -> {
                        Text(
                            "足跡を探しています…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.footprintLoadError != null -> {
                        Text(
                            state.footprintLoadError,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRetry) { Text("もう一度読む") }
                    }

                    state.footprints.isEmpty() -> {
                        Text(
                            "まだ灯はありません。あなたが最初の火を残せます。",
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
                        "次の誰かへ、ひとことだけ残せます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    FootprintPresets.all.take(6).chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = state.selectedFootprintPresetId == preset.id,
                                    onClick = { onSelectPreset(preset.id) },
                                    enabled = !state.footprintPosting,
                                    label = { Text("${preset.glyph} ${preset.text}", maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowPresets.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    state.footprintPostError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (state.footprintPosted) {
                Button(onClick = onDismiss) { Text("閉じる") }
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
                TextButton(onClick = onDismiss, enabled = !state.footprintPosting) {
                    Text("今は残さない")
                }
            }
        },
    )
}
