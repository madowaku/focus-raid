package com.madowaku.focusraid.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.model.SessionPhase

/**
 * v0.10c product-facing Focusing surface.
 *
 * The development label "PIXEL EXPEDITION" is intentionally absent. The world, location name and
 * the visible destination carry the journey instead. Session progress still comes exclusively from
 * FocusUiState.progress.
 */
@Composable
internal fun PixelExpeditionDestinationScreen(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestFinish: () -> Unit,
    progressOverride: Float? = null,
) {
    val paused = state.phase == SessionPhase.PAUSED
    val progress = (progressOverride ?: state.progress).coerceIn(0f, 1f)
    val stage = pixelExpeditionStage(progress)
    val lastMinute = !paused && state.remainingSeconds in 1..60

    val location = destinationLocationTitle(stage)
    val journeyCopy = destinationJourneyCopy(stage, paused, lastMinute)
    val statusCopy = when {
        paused -> "旅の途中で、ひと休み"
        lastMinute -> "あと1分、まもなく到着"
        else -> "${state.selectedMinutes}分の集中 · 遠征中"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070B12),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Focus Raid", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .90f),
                ) {
                    Text(
                        if (paused) "一時停止中" else "集中中",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                Text(
                    destinationClock(state.remainingSeconds),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "残り時間"
                            stateDescription = destinationClock(state.remainingSeconds)
                        },
                    textAlign = TextAlign.Center,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = Color(0xFFF3EDF9),
                )
                Text(
                    statusCopy,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = if (lastMinute) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (lastMinute) Color(0xFFFFC867) else Color(0xFFC7C0CF),
                )

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(292.dp)
                        .semantics {
                            contentDescription = "深層の遠征世界"
                            stateDescription = "$location ${(progress * 100).toInt()}パーセント"
                        },
                ) {
                    PixelExpeditionWorld(
                        progress = progress,
                        paused = paused,
                        modifier = Modifier.fillMaxSize(),
                    )

                }

                Spacer(Modifier.height(14.dp))
                Text(
                    location,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF4EFF8),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    journeyCopy,
                    fontSize = 13.sp,
                    color = Color(0xFFC7C0CF),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (state.worldSyncStatus) {
                        com.madowaku.focusraid.data.WorldSyncStatus.LOCAL_PREVIEW -> "体験プレビュー · 集中時間を端末に記録します。"
                        com.madowaku.focusraid.data.WorldSyncStatus.LIVE -> "今回の${state.selectedMinutes}分を、みんなの集中へ。完了後、共有レイドに送ります。"
                        else -> "集中は端末に記録。共有レイドへの反映には接続が必要です。"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFAFA8B7),
                )

                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (paused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("集中を再開", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text("Ⅱ  一時停止", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(
                onClick = onRequestFinish,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("セッションを終了", fontSize = 12.sp, color = Color(0xFFC7C0CF))
            }
        }
    }
}

internal fun destinationLocationTitle(stage: PixelExpeditionStage): String = when (stage) {
    PixelExpeditionStage.CAMP -> "夜明け前のキャンプ"
    PixelExpeditionStage.PATH -> "灯りの森道"
    PixelExpeditionStage.RIDGE -> "灰の稜線"
    PixelExpeditionStage.GATE -> "火口への石段"
    PixelExpeditionStage.RAID -> "火口の門"
}

internal fun destinationJourneyCopy(
    stage: PixelExpeditionStage,
    paused: Boolean,
    lastMinute: Boolean,
): String {
    if (paused) return when (stage) {
        PixelExpeditionStage.CAMP -> "ラグの卵と、ここから再開できます。"
        PixelExpeditionStage.PATH -> "ラグの卵と、森道の途中で休んでいます。"
        PixelExpeditionStage.RIDGE -> "ラグの卵と、稜線でひと休み。"
        PixelExpeditionStage.GATE -> "ラグの卵と、火口への石段でひと休み。"
        PixelExpeditionStage.RAID -> "ラグの卵と、門の前でひと休み。"
    }
    if (lastMinute) return "あと1分、ヴォルガの棲む火口へ。"
    return when (stage) {
        PixelExpeditionStage.CAMP -> "ラグの卵と、遠くに灯る火口へ。"
        PixelExpeditionStage.PATH -> "ラグの卵と、灯りをたどって森の奥へ。"
        PixelExpeditionStage.RIDGE -> "ラグの卵と、谷を渡る道の途中。"
        PixelExpeditionStage.GATE -> "ラグの卵と、火口へ続く石段を登る。"
        PixelExpeditionStage.RAID -> "ラグの卵と、ヴォルガの待つ門へ。"
    }
}

private fun destinationClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
