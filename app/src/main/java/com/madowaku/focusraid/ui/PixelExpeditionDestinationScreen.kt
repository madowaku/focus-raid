package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
                fontSize = 58.sp,
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
                DestinationPresenceOverlay(
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
                "環焔竜ヴォルガ · 完走すると、ボスへ一撃",
                fontSize = 11.sp,
                color = Color(0xFFAFA8B7),
            )

            Spacer(Modifier.weight(1f))
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

@Composable
private fun DestinationPresenceOverlay(
    progress: Float,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val safe = progress.coerceIn(0f, 1f)
    val reveal = ((safe - .52f) / .48f).coerceIn(0f, 1f)
    if (reveal <= 0f) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val alpha = reveal * if (paused) .32f else .68f
        val center = Offset(w * (.83f - reveal * .06f), h * .37f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF7A3D).copy(alpha = reveal * if (paused) .07f else .16f),
                    Color.Transparent,
                ),
                center = center,
                radius = h * (.20f + reveal * .06f),
            ),
            radius = h * (.20f + reveal * .06f),
            center = center,
        )

        // A deliberately incomplete silhouette: enough horn/wing/body language to promise Volga
        // without turning the quiet focus screen into a battle scene.
        val body = Path().apply {
            moveTo(center.x - w * .08f, center.y + h * .08f)
            lineTo(center.x - w * .02f, center.y - h * .06f)
            lineTo(center.x + w * .08f, center.y - h * .03f)
            lineTo(center.x + w * .13f, center.y + h * .08f)
            lineTo(center.x + w * .04f, center.y + h * .12f)
            close()
        }
        drawPath(body, Color(0xFF120F16).copy(alpha = alpha))

        val leftWing = Path().apply {
            moveTo(center.x - w * .01f, center.y)
            lineTo(center.x - w * .15f, center.y - h * .12f)
            lineTo(center.x - w * .11f, center.y + h * .03f)
            close()
        }
        drawPath(leftWing, Color(0xFF17111B).copy(alpha = alpha * .92f))

        val hornColor = Color(0xFF2B2028).copy(alpha = alpha)
        drawLine(hornColor, center, Offset(center.x - w * .035f, center.y - h * .10f), strokeWidth = 5f)
        drawLine(hornColor, center, Offset(center.x + w * .045f, center.y - h * .11f), strokeWidth = 5f)

        if (reveal > .55f) {
            val eyeAlpha = ((reveal - .55f) / .45f).coerceIn(0f, 1f) * if (paused) .34f else .92f
            drawCircle(Color(0xFFFF9A46).copy(alpha = eyeAlpha), radius = 3.2f, center = Offset(center.x - w * .024f, center.y - h * .015f))
            drawCircle(Color(0xFFFFC25C).copy(alpha = eyeAlpha * .55f), radius = 7.5f, center = Offset(center.x - w * .024f, center.y - h * .015f))
        }
    }
}

private fun destinationClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
