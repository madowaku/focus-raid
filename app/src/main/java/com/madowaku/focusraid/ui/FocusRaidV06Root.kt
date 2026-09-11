package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.data.ContributionStatus
import com.madowaku.focusraid.data.RaidEcho
import com.madowaku.focusraid.data.WorldSyncStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

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

    val showBeginningLight = state.phase == SessionPhase.COMPLETED &&
        reward != null &&
        reward.creditedMinutes > 0 &&
        first25Reserved &&
        state.persistenceError == null

    if (showBeginningLight) {
        BackHandler(enabled = !state.saving) {
            viewModel.resetAfterResult()
        }
        BeginningLightMoment(
            totalFocusMinutes = state.totalFocusMinutes,
            creditedMinutes = reward?.creditedMinutes ?: 25,
            companion = state.companion,
            onAgain = viewModel::startAgain,
            onDone = viewModel::resetAfterResult,
        )
        return
    }

    val firstRaidCandidate = state.phase == SessionPhase.COMPLETED &&
        reward != null &&
        reward.creditedMinutes > 0 &&
        !first25Reserved

    val currentContribution = state.resultSessionId?.let { resultId ->
        state.contributions.firstOrNull { it.sessionId == resultId }
    }
    val contributionAccepted = currentContribution?.state == ContributionStatus.ACCEPTED ||
        currentContribution?.state == ContributionStatus.ALREADY_COUNTED
    val contributionWaiting = currentContribution == null || currentContribution.state in setOf(
        ContributionStatus.PENDING,
        ContributionStatus.RETRYING,
    )

    var echoFeed by remember(state.resultSessionId) {
        mutableStateOf<FirstRaidEchoFeed>(FirstRaidEchoFeed.Idle)
    }
    var skipRaidSync by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }

    LaunchedEffect(
        state.resultSessionId,
        firstRaidCandidate,
        state.worldSyncStatus,
        currentContribution?.status,
        skipRaidSync,
    ) {
        if (!firstRaidCandidate) {
            echoFeed = FirstRaidEchoFeed.Idle
            return@LaunchedEffect
        }
        if (skipRaidSync) {
            echoFeed = FirstRaidEchoFeed.Failed
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
                delay(3_500)
                echoFeed = FirstRaidEchoFeed.Failed
            }

            state.worldSyncStatus != WorldSyncStatus.LIVE -> {
                echoFeed = FirstRaidEchoFeed.Failed
            }

            contributionAccepted -> {
                echoFeed = FirstRaidEchoFeed.Loading
                echoFeed = try {
                    val echoes = withTimeoutOrNull(5_000) { loadRaidEchoes() }
                    if (echoes == null) {
                        FirstRaidEchoFeed.Failed
                    } else {
                        FirstRaidEchoFeed.Ready(echoes = echoes, preview = false)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    FirstRaidEchoFeed.Failed
                }
            }

            contributionWaiting -> {
                echoFeed = FirstRaidEchoFeed.Loading
                delay(4_000)
                echoFeed = FirstRaidEchoFeed.Failed
            }

            else -> {
                echoFeed = FirstRaidEchoFeed.Failed
            }
        }
    }

    if (firstRaidCandidate && !skipRaidSync && echoFeed == FirstRaidEchoFeed.Loading) {
        FirstRaidEchoLoading(
            state = state,
            onSkip = { skipRaidSync = true },
        )
        return
    }

    val readyFeed = echoFeed as? FirstRaidEchoFeed.Ready
    val showFirstRaid = firstRaidCandidate && !skipRaidSync && readyFeed != null
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
    val playerDamage = if (resolvedFeed.preview) {
        completedReward.personalDamage.coerceAtLeast(0)
    } else {
        currentContribution?.appliedDamage?.coerceAtLeast(0) ?: 0
    }
    val recentEchoMinutes = resolvedFeed.echoes.sumOf { it.focusMinutes.coerceAtLeast(0) }
    var showFootprints by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }

    val scenario = ReturnRaidScenario.fromEchoes(
        bossName = state.world.bossName,
        bossHp = 181,
        bossMaxHp = 250,
        playerDamage = playerDamage,
        creditedMinutes = completedReward.creditedMinutes,
        echoes = resolvedFeed.echoes,
        chainCountBefore = resolvedFeed.echoes.size,
        chainMinutesBefore = recentEchoMinutes,
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
        FootprintDialog(
            state = state,
            onSelectPreset = viewModel::selectFootprintPreset,
            onLeaveFootprint = viewModel::leaveFootprint,
            onRetry = viewModel::retryFootprints,
            onDismiss = { showFootprints = false },
        )
    }
}

@Composable
internal fun FirstRaidEchoLoading(
    state: FocusUiState,
    onSkip: () -> Unit,
) {
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
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onSkip) {
            Text("待たずに記録を見る")
        }
    }
}

private fun previewRaidEchoes(): List<RaidEcho> = listOf(
    RaidEcho("3時間前", 25, 25),
    RaidEcho("51分前", 50, 50),
    RaidEcho("12分前", 25, 25),
)
