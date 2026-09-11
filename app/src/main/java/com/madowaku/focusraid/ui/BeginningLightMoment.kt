package com.madowaku.focusraid.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.BeginningLight
import com.madowaku.focusraid.core.domain.CompanionGrowth
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.domain.CompanionStage

@Composable
internal fun BeginningLightMoment(
    totalFocusMinutes: Int,
    creditedMinutes: Int,
    companion: CompanionIdentity,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val status = BeginningLight.from(totalFocusMinutes)
    var touched by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val glow by animateFloatAsState(
        targetValue = if (status.hatched || touched) 1f else .58f,
        animationSpec = tween(durationMillis = 420),
        label = "beginning-light-glow",
    )
    val displayStage = if (status.hatched) {
        CompanionGrowth.from(totalFocusMinutes).stage
    } else {
        CompanionStage.EGG
    }

    CompositionLocalProvider(LocalCompanionIdentity provides companion) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .35f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "FIRST 25",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "はじまりの灯",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (status.hatched) {
                        "集中の積み重ねが25分を越え、最初の灯がそのまま孵化の光になりました。"
                    } else {
                        "集中の積み重ねが25分に届き、相棒のそばに小さな灯を残しました。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "今回 +${creditedMinutes.coerceAtLeast(1)}分",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = .28f * glow),
                                    MaterialTheme.colorScheme.primary.copy(alpha = .12f * glow),
                                    MaterialTheme.colorScheme.surface.copy(alpha = .04f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val companionModifier = Modifier
                        .size(160.dp)
                        .testTag(if (status.hatched) "beginning_light_hatched" else "beginning_light_egg")
                        .then(
                            if (status.hatched) {
                                Modifier
                            } else {
                                Modifier.clickable {
                                    touched = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                        )
                    Surface(
                        modifier = companionModifier,
                        shape = CircleShape,
                        color = if (status.hatched || touched) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .84f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = .88f)
                        },
                        tonalElevation = if (status.hatched || touched) 10.dp else 3.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CompanionArtwork(
                                modifier = Modifier.size(124.dp),
                                stage = displayStage,
                                mood = CompanionMood.Idle,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        status.hatched -> "灯の向こうで、相棒が目を開いた。"
                        touched -> "…こつん。"
                        else -> "卵に触れてみる"
                    },
                    fontSize = if (status.hatched || touched) 20.sp else 13.sp,
                    fontWeight = if (status.hatched || touched) FontWeight.Bold else FontWeight.Normal,
                    color = if (status.hatched || touched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (touched && !status.hatched) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "灯の色が、ほんの少し変わった。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                    ),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("孵化までの道のり", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        BeginningLightProgress(status.hatchProgress)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${status.totalMinutes.coerceAtMost(75)} / 75分",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            when {
                                status.hatched -> "孵化しました。最初の灯は相棒画面に残ります。"
                                status.hatchRemainingMinutes > 0 -> "あと${status.hatchRemainingMinutes}分。灯は相棒画面に残ります。"
                                else -> "孵化の時間に到達しました。"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Text("もう25分", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDone) {
                    Text("今日はここまで")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun BeginningLightCard(totalFocusMinutes: Int) {
    val status = BeginningLight.from(totalFocusMinutes)
    if (!status.lit) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .78f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                "🔥  はじまりの灯",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "最初の25分で灯った、あなたと相棒のしるし。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
            )
            Spacer(Modifier.height(10.dp))
            BeginningLightProgress(status.hatchProgress)
            Spacer(Modifier.height(7.dp))
            Text(
                if (status.hatchRemainingMinutes > 0) {
                    "孵化まであと${status.hatchRemainingMinutes}分"
                } else {
                    "最初の灯は、これからもここに残ります。"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun BeginningLightProgress(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
    }
}
