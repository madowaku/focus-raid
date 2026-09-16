package com.madowaku.focusraid.ui

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * v0.10a modern HUD over the procedural Pixel Expedition world.
 *
 * The session remains owned by FocusViewModel. This screen is presentation-only and derives every
 * stage/camera change from FocusUiState.progress.
 */
@Composable
internal fun PixelExpeditionFocusingScreen(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRequestFinish: () -> Unit,
    progressOverride: Float? = null,
) {
    val paused = state.phase == SessionPhase.PAUSED
    val progress = (progressOverride ?: state.progress).coerceIn(0f, 1f)
    val lastMinute = !paused && state.remainingSeconds in 1..60
    val status = when {
        lastMinute -> "レイド地点の灯が見えてきた"
        else -> pixelExpeditionStatusCopy(progress, paused)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                    color = if (paused) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .90f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .90f)
                    },
                ) {
                    Text(
                        if (paused) "一時停止中" else "集中中",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                pixelClock(state.remainingSeconds),
                modifier = Modifier.semantics {
                    contentDescription = "残り時間"
                    stateDescription = pixelClock(state.remainingSeconds)
                },
                fontSize = 56.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
            Text(
                status,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = if (lastMinute) FontWeight.Bold else FontWeight.Normal,
                color = if (lastMinute) {
                    Color(0xFFFFCB67)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .heightIn(min = 280.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF0A101B),
                tonalElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.semantics {
                        contentDescription = "集中の遠征世界"
                        stateDescription = "${pixelExpeditionStage(progress).name.lowercase()} ${(progress * 100).toInt()}パーセント"
                    },
                ) {
                    PixelExpeditionWorld(
                        progress = progress,
                        paused = paused,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PixelWorldStageChip(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            PixelBossHud(state)
            Spacer(Modifier.height(12.dp))

            if (paused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text("▶  集中を再開", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text("Ⅱ  一時停止", fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onRequestFinish) {
                Text("セッションを終了", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PixelWorldStageChip(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val stage = pixelExpeditionStage(progress)
    val label = when (stage) {
        PixelExpeditionStage.CAMP -> "CAMP · 出発"
        PixelExpeditionStage.PATH -> "PATH · 森道"
        PixelExpeditionStage.RIDGE -> "RIDGE · 山道"
        PixelExpeditionStage.GATE -> "GATE · 門"
        PixelExpeditionStage.RAID -> "RAID · 到着間近"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xB3121724),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE8E2F4),
        )
    }
}

@Composable
private fun PixelBossHud(state: FocusUiState) {
    val maxHp = state.world.bossMaxHp.coerceAtLeast(0)
    val hp = state.world.bossHp.coerceIn(0, maxHp.takeIf { it > 0 } ?: Int.MAX_VALUE)
    val fraction = if (maxHp <= 0) 0f else (hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xD9141720),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.world.bossName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (state.world.bossHp <= 0) "討伐完了" else "完走すると、この遠征が一撃になる",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${(fraction * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFCB67),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282533)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFB79AFF),
                                    Color(0xFFFF7D8C),
                                    Color(0xFFFFCB67),
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

private fun pixelClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
