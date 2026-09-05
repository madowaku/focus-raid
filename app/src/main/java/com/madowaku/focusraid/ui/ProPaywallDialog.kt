package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Text(if (isPro) "Focus Raid Pro" else "遠征範囲をすべて解放")
        },
        text = {
            Column {
                if (isPro) {
                    Text("✓ Focus Raid Pro は解放済みです。")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "星渡り航路を含むPro向けRaid、テーマ、詳細統計、全期間履歴、カスタマイズを利用できます。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Focus Raid Pro · 買い切り")
                    Spacer(Modifier.height(14.dp))
                    Text("✦  Pro Raid『星渡り航路』")
                    Text("🗺  今後追加されるPro Raid")
                    Text("📊  詳細統計")
                    Text("🗃  全期間の履歴")
                    Text("🎨  すべてのテーマとカスタマイズ")
                    Text("👣  足跡カスタマイズ")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "集中に必要な基本機能はFreeのまま使えます。Proは世界と記録を深く楽しむための追加解放です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    when (purchaseState) {
                        is PurchaseState.Error -> Text(
                            purchaseState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )

                        PurchaseState.Restoring -> Text(
                            "購入履歴を確認しています…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        PurchaseState.Success -> Text(
                            "復元できるPro購入は見つかりませんでした。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        else -> access.errorMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isPro) {
                Button(onClick = onDismiss) {
                    Text("閉じる")
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
                    )
                }
            }
        },
        dismissButton = {
            if (!isPro) {
                TextButton(
                    onClick = onRestore,
                    enabled = !busy,
                ) {
                    Text(if (purchaseState == PurchaseState.Restoring) "復元中…" else "購入を復元")
                }
            }
        },
    )
}
