package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.BuildConfig
import com.madowaku.focusraid.billing.ProAccessViewModel
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.SessionOutcome
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.data.ContributionStatus
import com.madowaku.focusraid.data.RaidEcho
import com.madowaku.focusraid.data.WorldSyncStatus
import kotlinx.coroutines.CancellationException

private sealed interface FirstRaidEchoFeed {
    data object Idle : FirstRaidEchoFeed
    data object Loading : FirstRaidEchoFeed
    data object Failed : FirstRaidEchoFeed
    data class Ready(
        val echoes: List<RaidEcho>,
        val preview: Boolean,
    ) : FirstRaidEchoFeed
}

@Composable
fun FocusRaidV06Root(
    viewModel: FocusViewModel,
    proAccessViewModel: ProAccessViewModel,
    systemAccess: FocusSystemAccess = FocusSystemAccess(),
    loadRaidEchoes: suspend () -> List<RaidEcho> = { emptyList() },
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
    val firstRaidCandidate = state.phase == SessionPhase.COMPLETED &&
        reward != null &&
        reward.creditedMinutes > 0 &&
        !first25Reserved
    val currentContribution = state.resultSessionId?.let { resultId ->
        state.contributions.firstOrNull { it.sessionId == resultId }
    }
    val contributionAccepted = currentContribution?.state == ContributionStatus.ACCEPTED ||
        currentContribution?.state == ContributionStatus.ALREADY_COUNTED
    val contributionPending = currentContribution == null || currentContribution.state in setOf(
        ContributionStatus.PENDING,
        ContributionStatus.RETRYING,
        ContributionStatus.OFFLINE,
        ContributionStatus.AUTH_REQUIRED,
    )

    var echoFeed by remember(state.resultSessionId) {
        mutableStateOf<FirstRaidEchoFeed>(FirstRaidEchoFeed.Idle)
    }
    LaunchedEffect(
        state.resultSessionId,
        firstRaidCandidate,
        state.worldSyncStatus,
        currentContribution?.status,
    ) {
        if (!firstRaidCandidate) {
            echoFeed = FirstRaidEchoFeed.Idle
            return@LaunchedEffect
        }
        when {
            BuildConfig.DEBUG && state.worldSyncStatus in setOf(
                WorldSyncStatus.LOCAL_PREVIEW,
                WorldSyncStatus.OFFLINE,
            ) -> {
                echoFeed = FirstRaidEchoFeed.Ready(previewRaidEchoes(), preview = true)
            }

            state.worldSyncStatus == WorldSyncStatus.CONNECTING -> {
                echoFeed = FirstRaidEchoFeed.Loading
            }

            state.worldSyncStatus != WorldSyncStatus.LIVE -> {
                echoFeed = FirstRaidEchoFeed.Failed
            }

            contributionAccepted -> {
                echoFeed = FirstRaidEchoFeed.Loading
                echoFeed = try {
                    FirstRaidEchoFeed.Ready(loadRaidEchoes(), preview = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    FirstRaidEchoFeed.Failed
                }
            }

            contributionPending -> {
                echoFeed = FirstRaidEchoFeed.Loading
            }

            else -> {
                echoFeed = FirstRaidEchoFeed.Failed
            }
        }
    }

    if (firstRaidCandidate && echoFeed == FirstRaidEchoFeed.Loading) {
        FirstRaidEchoLoading(state)
        return
    }

    val readyFeed = echoFeed as? FirstRaidEchoFeed.Ready
    val showFirstRaid = firstRaidCandidate && readyFeed != null
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

    val resolvedFeed = checkNotNull(readyFeed)
    val completedReward = checkNotNull(reward)
    val completedSessions = state.sessionHistory.count { it.outcome == SessionOutcome.COMPLETED }
    val currentRecorded = state.resultSessionId?.let { id ->
        state.sessionHistory.any { it.sessionId == id && it.outcome == SessionOutcome.COMPLETED }
    } == true
    val chainCountBefore = (completedSessions - if (currentRecorded) 1 else 0).coerceAtLeast(0)
    val chainMinutesBefore = (state.totalFocusMinutes - completedReward.creditedMinutes).coerceAtLeast(0)
    val playerDamage = if (resolvedFeed.preview) {
        completedReward.personalDamage.coerceAtLeast(0)
    } else {
        currentContribution?.appliedDamage?.coerceAtLeast(0) ?: 0
    }
    var showFootprints by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }
    val scenario = ReturnRaidScenario.fromEchoes(
        bossName = state.world.bossName,
        bossHp = 181,
        bossMaxHp = 250,
        playerDamage = playerDamage,
        creditedMinutes = completedReward.creditedMinutes,
        echoes = resolvedFeed.echoes,
        chainCountBefore = chainCountBefore,
        chainMinutesBefore = chainMinutesBefore,
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
private fun FirstRaidEchoLoading(state: FocusUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "帰還しました",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "${state.reward?.creditedMinutes ?: 0}分",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "遠征記録を同期しています…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun previewRaidEchoes(): List<RaidEcho> = listOf(
    RaidEcho("3時間前", 25, 25),
    RaidEcho("51分前", 50, 50),
    RaidEcho("12分前", 25, 25),
)

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
