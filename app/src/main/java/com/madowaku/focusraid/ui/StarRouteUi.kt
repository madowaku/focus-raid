package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.StarRoute
import kotlin.math.max

@Composable
internal fun StarRouteLaunchCard(state: FocusUiState) {
    val checkpoint = StarRoute.targetCheckpoint(state.totalFocusMinutes)
    val minutesUntilTarget = StarRoute.minutesUntilTarget(state.totalFocusMinutes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .84f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "✦  PRO EXPEDITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .72f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "星渡り航路",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "次の目的地 · 第${checkpoint}星標 · あと${minutesUntilTarget}分",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
            )
            Spacer(Modifier.height(10.dp))
            StarBeaconRoute(0)
            Spacer(Modifier.height(4.dp))
            Text(
                "集中すると5つの航路灯が順に灯ります。操作は不要です。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
            )
        }
    }
}

@Composable
internal fun StarRouteProgressCard(state: FocusUiState) {
    val litBeacons = StarRoute.litBeacons(state.progress)
    val elapsedMinutes = max(0, (state.durationSeconds - state.remainingSeconds) / 60)
    val accumulatedMinutes = state.totalFocusMinutes + elapsedMinutes
    val checkpoint = StarRoute.targetCheckpoint(accumulatedMinutes)
    val minutesUntilTarget = StarRoute.minutesUntilTarget(accumulatedMinutes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .82f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "PRO EXPEDITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .72f),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "星渡り航路 · 第${checkpoint}星標へ",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            StarBeaconRoute(litBeacons)
            Spacer(Modifier.height(8.dp))
            Text(
                "$litBeacons / ${StarRoute.BEACONS_PER_ROUTE} 航路灯点灯  ·  あと${minutesUntilTarget}分",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
            )
        }
    }
}

@Composable
internal fun StarRouteVictoryCard(state: FocusUiState) {
    val creditedMinutes = state.reward?.creditedMinutes?.coerceAtLeast(0) ?: 0
    val beforeTotalMinutes = (state.totalFocusMinutes - creditedMinutes).coerceAtLeast(0)
    val beforeCheckpoint = StarRoute.reachedCheckpoint(beforeTotalMinutes)
    val reachedCheckpoint = StarRoute.reachedCheckpoint(state.totalFocusMinutes)
    val newlyReached = (reachedCheckpoint - beforeCheckpoint).coerceAtLeast(0)
    val targetCheckpoint = StarRoute.targetCheckpoint(state.totalFocusMinutes)
    val minutesUntilTarget = StarRoute.minutesUntilTarget(state.totalFocusMinutes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .88f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                "✦  STAR ROUTE COMPLETE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (newlyReached > 0) {
                    "第${reachedCheckpoint}星標へ到達"
                } else {
                    "第${targetCheckpoint}星標へ航行中"
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            StarBeaconRoute(StarRoute.BEACONS_PER_ROUTE)
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    newlyReached > 1 -> "一度の集中で${newlyReached}つの星標を越えました。"
                    newlyReached == 1 -> "集中した時間が、新しい星標を照らしました。"
                    else -> "次の星標まであと${minutesUntilTarget}分。航路はここから続きます。"
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
            )
        }
    }
}

@Composable
private fun StarBeaconRoute(litBeacons: Int) {
    val lit = litBeacons.coerceIn(0, StarRoute.BEACONS_PER_ROUTE)
    val activeColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
    ) {
        val count = StarRoute.BEACONS_PER_ROUTE
        val left = 12.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val step = if (count <= 1) 0f else (right - left) / (count - 1)
        val centerY = size.height / 2f

        drawLine(
            color = inactiveColor,
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )

        if (lit > 1) {
            drawLine(
                color = activeColor,
                start = Offset(left, centerY),
                end = Offset(left + step * (lit - 1), centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        repeat(count) { index ->
            val center = Offset(left + step * index, centerY)
            drawCircle(
                color = if (index < lit) activeColor else inactiveColor,
                radius = if (index < lit) 7.dp.toPx() else 5.dp.toPx(),
                center = center,
            )
        }
    }
}
