package com.madowaku.focusraid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.ui.FocusUiState
import com.madowaku.focusraid.ui.PixelExpeditionFocusingScreen
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

/** Debug-only deterministic entry point for v0.10 screenshot QA. */
class PixelExpeditionQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phase = intent.getStringExtra(EXTRA_PHASE).orEmpty().uppercase()
        val sample = qaSample(phase)

        setContent {
            FocusRaidTheme {
                PixelExpeditionFocusingScreen(
                    state = sample.state,
                    progressOverride = sample.progress,
                    onPause = {},
                    onResume = {},
                    onRequestFinish = {},
                )
            }
        }
    }

    private fun qaSample(phase: String): PixelQaSample = when (phase) {
        "PIXEL_FOCUS_12_30" -> PixelQaSample(
            state = baseState(remainingSeconds = 12 * 60 + 30),
            progress = .50f,
        )
        "PIXEL_FOCUS_00_59" -> PixelQaSample(
            state = baseState(remainingSeconds = 59),
            progress = .96f,
        )
        "PIXEL_FOCUS_PAUSED" -> PixelQaSample(
            state = baseState(remainingSeconds = 12 * 60 + 30, phase = SessionPhase.PAUSED),
            progress = .50f,
        )
        else -> PixelQaSample(
            state = baseState(remainingSeconds = 25 * 60),
            progress = 0f,
        )
    }

    private fun baseState(
        remainingSeconds: Int,
        phase: SessionPhase = SessionPhase.RUNNING,
    ): FocusUiState = FocusUiState(
        initialized = true,
        phase = phase,
        selectedMinutes = 25,
        remainingSeconds = remainingSeconds,
        durationSeconds = 25 * 60,
        expedition = Expedition.ABYSS,
        totalFocusMinutes = 50,
        world = WorldSnapshot(
            bossName = "環焔竜ヴォルガ",
            bossHp = 428_192,
            bossMaxHp = 650_000,
        ),
    )

    private data class PixelQaSample(
        val state: FocusUiState,
        val progress: Float,
    )

    companion object {
        const val EXTRA_PHASE = "phase"
    }
}
