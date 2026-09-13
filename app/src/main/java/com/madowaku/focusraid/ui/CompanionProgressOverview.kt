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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.CompanionGrowth
import com.madowaku.focusraid.core.domain.CompanionGrowthStatus
import com.madowaku.focusraid.core.domain.CompanionStage

@Composable
internal fun CompanionProgressOverview(
    state: FocusUiState,
    onSelect: (com.madowaku.focusraid.core.domain.CompanionIdentity) -> Unit = {},
) {
    val growth = CompanionGrowth.from(state.totalFocusMinutes)
    val totalHours = growth.totalMinutes / 60
    val totalRemainderMinutes = growth.totalMinutes % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "COMPANIONS",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "集中すると、相棒も育つ",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))
        CompanionSummaryCard(
            state = state,
            growth = growth,
            totalHours = totalHours,
            totalRemainderMinutes = totalRemainderMinutes,
        )
        CompanionRoster(state, onSelect)
        Spacer(Modifier.height(10.dp))
        GrowthFormsCard(currentStage = growth.stage)
        InventoryCard(state.sessionHistory)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CompanionSummaryCard(
    state: FocusUiState,
    growth: CompanionGrowthStatus,
    totalHours: Int,
    totalRemainderMinutes: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompanionArtwork(
                    modifier = Modifier.size(106.dp),
                    stage = growth.stage,
                    mood = CompanionMood.Idle,
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.companion.label, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            growth.stage.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "累計集中時間",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${totalHours}h ${totalRemainderMinutes}m",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    growth.nextStageLabel?.let { "あと${growth.remainingMinutes}分で$it" } ?: "最大まで成長",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            GrowthBar(growth.progress)
        }
    }
}

@Composable
private fun GrowthFormsCard(currentStage: CompanionStage) {
    val stages = CompanionStage.entries
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(
                "成長の姿",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                stages.forEach { stage ->
                    val unlocked = stage.ordinal <= currentStage.ordinal
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (unlocked) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .76f)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (unlocked) {
                                CompanionArtwork(
                                    modifier = Modifier.size(46.dp),
                                    stage = stage,
                                )
                            } else {
                                Text("🔒", fontSize = 17.sp)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (unlocked) stage.label else "???",
                            fontSize = 9.sp,
                            fontWeight = if (stage == currentStage) FontWeight.Bold else FontWeight.Normal,
                            color = if (stage == currentStage) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
