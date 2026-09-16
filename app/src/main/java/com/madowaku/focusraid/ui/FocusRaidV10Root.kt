package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.ProAccessViewModel
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase

/**
 * v0.10 entry point.
 *
 * The first Pixel Expedition vertical slice belongs to 深層 + ラグ only. Other expeditions and
 * companions deliberately stay on the established v0.8 shell until they get their own world art
 * and sprite spec.
 */
@Composable
fun FocusRaidV10Root(
    viewModel: FocusViewModel,
    proAccessViewModel: ProAccessViewModel,
    systemAccess: FocusSystemAccess = FocusSystemAccess(),
    loadRaidEchoes: suspend () -> List<com.madowaku.focusraid.data.RaidEcho> = { emptyList() },
    onRequestNotificationPermission: () -> Unit = {},
    onRequestExactAlarmPermission: () -> Unit = {},
    onPurchasePro: () -> Unit = {},
    onRestorePurchases: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusing = state.phase == SessionPhase.RUNNING || state.phase == SessionPhase.PAUSED
    val pixelSliceActive = focusing &&
        state.expedition == Expedition.ABYSS &&
        state.companion == CompanionIdentity.RAG
    var showEndConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.phase, state.expedition, state.companion) {
        if (!pixelSliceActive) showEndConfirmation = false
    }

    if (pixelSliceActive) {
        BackHandler(enabled = !state.saving && state.persistenceError == null) {
            showEndConfirmation = true
        }

        PixelExpeditionFocusingScreen(
            state = state,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onRequestFinish = { showEndConfirmation = true },
        )

        if (showEndConfirmation) {
            AlertDialog(
                onDismissRequest = { showEndConfirmation = false },
                title = { Text("この遠征をここで終えますか？") },
                text = { Text("ここまでの集中時間は記録されます。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEndConfirmation = false
                            viewModel.finishEarly()
                        },
                    ) { Text("終了する") }
                },
                dismissButton = {
                    TextButton(onClick = { showEndConfirmation = false }) { Text("続ける") }
                },
            )
        }
        return
    }

    FocusRaidV08Root(
        viewModel = viewModel,
        proAccessViewModel = proAccessViewModel,
        systemAccess = systemAccess,
        loadRaidEchoes = loadRaidEchoes,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onRequestExactAlarmPermission = onRequestExactAlarmPermission,
        onPurchasePro = onPurchasePro,
        onRestorePurchases = onRestorePurchases,
    )
}
