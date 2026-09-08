package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.billing.ProAccessState
import com.madowaku.focusraid.billing.PurchaseState

@Composable
internal fun ProPaywallDialog(
    access: ProAccessState,
    purchaseState: PurchaseState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isPro = access.accessLevel == AccessLevel.PRO
    val busy = purchaseState == PurchaseState.Purchasing ||
        purchaseState == PurchaseState.Restoring

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        "✦",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.padding(top = 5.dp))
                Text(
                    "Focus Raid Pro",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                )
                if (!isPro) {
                    Text(
                        "買い切り · サブスクなし",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        text = {
            Column {
                if (isPro) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "✓  PRO UNLOCKED",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "星渡り航路・詳細統計・全期間履歴を利用できます。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .78f),
                            )
                        }
                    }
                } else {
                    Text(
                        "集中はそのまま。冒険と記録を、もう一段深く。",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.padding(top = 7.dp))

                    ProBenefit(
                        glyph = "✦",
                        title = "星渡り航路",
                        body = "5つの星標を灯しながら進むPro専用遠征",
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    ProBenefit(
                        glyph = "▥",
                        title = "詳細統計",
                        body = "完走率・平均時間・最長記録・遠征別の集中時間",
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    ProBenefit(
                        glyph = "▤",
                        title = "全期間の履歴",
                        body = "Freeの7日間を越えて、冒険記録をすべて振り返る",
                    )

                    Spacer(Modifier.padding(top = 7.dp))
                    Text(
                        "基本タイマー、天空塔、深層迷宮はFreeのまま使えます。Proは世界と記録を深く楽しむための追加解放です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    PurchaseFeedback(
                        access = access,
                        purchaseState = purchaseState,
                    )
                }
            }
        },
        confirmButton = {
            if (isPro) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("冒険へ戻る")
                }
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = !busy && access.product != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            purchaseState == PurchaseState.Purchasing -> "購入処理中…"
                            access.refreshing -> "価格を確認中…"
                            access.product != null -> "${access.product.formattedPrice} でProを解放"
                            else -> "商品情報を確認できません"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            if (!isPro) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = onRestore,
                        enabled = !busy,
                    ) {
                        Text(if (purchaseState == PurchaseState.Restoring) "復元中…" else "購入を復元")
                    }
                }
            }
        },
    )
}

@Composable
private fun ProBenefit(
    glyph: String,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    glyph,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    body,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PurchaseFeedback(
    access: ProAccessState,
    purchaseState: PurchaseState,
) {
    when (purchaseState) {
        is PurchaseState.Error -> {
            Spacer(Modifier.padding(top = 5.dp))
            Text(
                purchaseState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        PurchaseState.Restoring -> {
            Spacer(Modifier.padding(top = 5.dp))
            Text(
                "購入履歴を確認しています…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PurchaseState.Success -> {
            Spacer(Modifier.padding(top = 5.dp))
            Text(
                "復元できるPro購入は見つかりませんでした。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> access.errorMessage?.let { message ->
            Spacer(Modifier.padding(top = 5.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
