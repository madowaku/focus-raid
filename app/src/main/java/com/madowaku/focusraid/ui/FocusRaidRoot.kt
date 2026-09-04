package com.madowaku.focusraid.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.core.model.SessionPhase

@Composable
fun FocusRaidRoot(viewModel: FocusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showCustomDuration by rememberSaveable { mutableStateOf(false) }
    var customDurationMinutes by rememberSaveable { mutableStateOf(state.selectedMinutes) }
    var showEndConfirmation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.phase) {
        if (state.phase != SessionPhase.RUNNING && state.phase != SessionPhase.PAUSED) {
            showEndConfirmation = false
        }
        if (state.phase != SessionPhase.READY) {
            showCustomDuration = false
        }
    }

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
            onSelectMinutes = viewModel::selectMinutes,
            onSelectExpedition = viewModel::selectExpedition,
            onTimerClick = {
                customDurationMinutes = state.selectedMinutes
                showCustomDuration = true
            },
            onStart = viewModel::start,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onFinishEarly = { showEndConfirmation = true },
            onAgain = viewModel::startAgain,
            onDone = viewModel::resetAfterResult,
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
