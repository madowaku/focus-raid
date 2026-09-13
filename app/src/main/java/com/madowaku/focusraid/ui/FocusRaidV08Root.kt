package com.madowaku.focusraid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.ProAccessViewModel

/** v0.8 entry point: the first-run world moment sits in front of the established v0.7 shell. */
@Composable
fun FocusRaidV08Root(
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

    FocusRaidV07Root(
        viewModel = viewModel,
        proAccessViewModel = proAccessViewModel,
        systemAccess = systemAccess,
        loadRaidEchoes = loadRaidEchoes,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onRequestExactAlarmPermission = onRequestExactAlarmPermission,
        onPurchasePro = onPurchasePro,
        onRestorePurchases = onRestorePurchases,
    )

    if (shouldShowFirstRun(state)) {
        FirstRunMoment(
            state = state,
            onSkip = viewModel::completeFirstRun,
            onComplete = viewModel::completeFirstRun,
            onSelfStrike = viewModel::firstRunSelfStrike,
            onOtherLight = viewModel::firstRunOtherLight,
        )
    }
}
