package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CustomDurationSheet(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "集中時間",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onMinutesChange((minutes - 1).coerceAtLeast(MIN_MINUTES)) },
                    enabled = minutes > MIN_MINUTES,
                    modifier = Modifier.size(width = 64.dp, height = 52.dp),
                ) {
                    Text("−", fontSize = 24.sp)
                }

                Text(
                    text = "$minutes 分",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Button(
                    onClick = { onMinutesChange((minutes + 1).coerceAtMost(MAX_MINUTES)) },
                    enabled = minutes < MAX_MINUTES,
                    modifier = Modifier.size(width = 64.dp, height = 52.dp),
                ) {
                    Text("＋", fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "5〜180分・1分単位",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESETS.forEach { preset ->
                    FilterChip(
                        selected = minutes == preset,
                        onClick = { onMinutesChange(preset) },
                        label = {
                            Text(
                                text = preset.toString(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(
                    text = "$minutes 分に設定",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 180
private val PRESETS = listOf(15, 25, 45, 60)
