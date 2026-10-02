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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private sealed interface SignatureFirstRaidEchoFeed {
    data object Idle : SignatureFirstRaidEchoFeed
    data object Loading : SignatureFirstRaidEchoFeed
    data object Failed : SignatureFirstRaidEchoFeed
    data class Ready(
        val echoes: List<RaidEcho>,
        val preview: Boolean,
        val world: com.madowaku.focusraid.core.model.WorldSnapshot? = null,
    ) : SignatureFirstRaidEchoFeed
}

/** v0.7 entry point. FIRST 25 and first-raid return sequences stay reserved exactly as in v0.6. */
@Composable
fun FocusRaidV07Root(
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
            onKnock = viewModel::companionKnock,
            onAgain = viewModel::startAgain,
            onDone = viewModel::confirmResultAndReset,
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
        mutableStateOf<SignatureFirstRaidEchoFeed>(SignatureFirstRaidEchoFeed.Idle)
    }
    var skipRaidSync by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }

    LaunchedEffect(
        state.resultSessionId,
        firstRaidCandidate,
        currentContribution?.status,
        skipRaidSync,
    ) {
        if (!firstRaidCandidate) {
            echoFeed = SignatureFirstRaidEchoFeed.Idle
            return@LaunchedEffect
        }
        if (skipRaidSync) {
            echoFeed = SignatureFirstRaidEchoFeed.Failed
            return@LaunchedEffect
        }

        echoFeed = SignatureFirstRaidEchoFeed.Loading
        val connection = if (state.worldSyncStatus == WorldSyncStatus.CONNECTING) {
            withTimeoutOrNull(4_000) {
                viewModel.uiState.first { it.worldSyncStatus != WorldSyncStatus.CONNECTING }.worldSyncStatus
            } ?: WorldSyncStatus.OFFLINE
        } else state.worldSyncStatus
        when {
            BuildConfig.DEBUG && connection == WorldSyncStatus.LOCAL_PREVIEW -> {
                echoFeed = SignatureFirstRaidEchoFeed.Ready(signaturePreviewRaidEchoes(), preview = true)
            }
            connection != WorldSyncStatus.LIVE -> { echoFeed = SignatureFirstRaidEchoFeed.Failed }
            contributionAccepted -> {
                echoFeed = SignatureFirstRaidEchoFeed.Loading
                echoFeed = try {
                    val shared = viewModel.refreshRaidSnapshot(currentContribution?.generation)
                    val echoes = if (shared != null) withTimeoutOrNull(5_000) { loadRaidEchoes() } else null
                    if (echoes == null || shared == null) {
                        SignatureFirstRaidEchoFeed.Failed
                    } else {
                        SignatureFirstRaidEchoFeed.Ready(echoes = echoes, preview = false, world = shared)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    SignatureFirstRaidEchoFeed.Failed
                }
            }

            contributionWaiting -> {
                echoFeed = SignatureFirstRaidEchoFeed.Loading
                delay(4_000)
                echoFeed = SignatureFirstRaidEchoFeed.Failed
            }

            else -> {
                echoFeed = SignatureFirstRaidEchoFeed.Failed
            }
        }
    }

    if (
        firstRaidCandidate &&
        !skipRaidSync &&
        echoFeed == SignatureFirstRaidEchoFeed.Loading
    ) {
        SignatureFirstRaidEchoLoading(
            state = state,
            onSkip = { skipRaidSync = true },
        )
        return
    }

    val readyFeed = echoFeed as? SignatureFirstRaidEchoFeed.Ready
    val showFirstRaid = firstRaidCandidate && !skipRaidSync && readyFeed != null
    if (!showFirstRaid) {
        FocusRaidSignatureRoot(
            viewModel = viewModel,
            proAccessViewModel = proAccessViewModel,
            systemAccess = systemAccess,
            loadRecentRaidEchoes = loadRaidEchoes,
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
    var showFootprints by rememberSaveable(state.resultSessionId) { mutableStateOf(false) }

    val scenario = if (resolvedFeed.preview) {
        ReturnRaidScenario.fromEchoes(
            bossName = state.world.bossName, bossHp = 181, bossMaxHp = 250,
            playerDamage = playerDamage, creditedMinutes = completedReward.creditedMinutes,
            echoes = resolvedFeed.echoes, chainCountBefore = resolvedFeed.echoes.take(2).size,
            chainMinutesBefore = resolvedFeed.echoes.take(2).sumOf { it.focusMinutes.coerceAtLeast(0) },
        ).copy(presentationLabel = "体験プレビュー · 人数・HPはサンプル")
    } else {
        ReturnRaidScenario.fromSharedSnapshot(checkNotNull(resolvedFeed.world), playerDamage,
            completedReward.creditedMinutes, resolvedFeed.echoes)
    }

    BackHandler(enabled = !state.saving) {
        viewModel.resetAfterResult()
    }

    val usePixelReturnRaid = state.expedition == com.madowaku.focusraid.core.model.Expedition.ABYSS &&
        state.companion == com.madowaku.focusraid.core.domain.CompanionIdentity.RAG

    if (usePixelReturnRaid) {
        PixelReturnRaidSequence(
            scenario = scenario,
            onEchoAudio = viewModel::raidEcho,
            onStrikeAudio = viewModel::raidSelfStrike,
            onVictoryAudio = viewModel::raidVictory,
            onFootprints = { showFootprints = true },
            onAgain = viewModel::startAgain,
            onDone = viewModel::confirmResultAndReset,
        )
    } else {
        ReturnRaidSequence(
            scenario = scenario,
            onEchoAudio = viewModel::raidEcho,
            onStrikeAudio = viewModel::raidSelfStrike,
            onVictoryAudio = viewModel::raidVictory,
            onFootprints = { showFootprints = true },
            onAgain = viewModel::startAgain,
            onDone = viewModel::confirmResultAndReset,
        )
    }

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
private fun SignatureFirstRaidEchoLoading(
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

private fun signaturePreviewRaidEchoes(): List<RaidEcho> = listOf(
    RaidEcho("3時間前", 25, 25),
    RaidEcho("51分前", 50, 50),
    RaidEcho("12分前", 25, 25),
)
