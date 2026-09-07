package com.madowaku.focusraid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.madowaku.focusraid.data.WorldSyncStatus

@Composable
internal fun WorldStatusLabel(status: WorldSyncStatus) {
    Text(when (status) {
        WorldSyncStatus.LOCAL_PREVIEW -> "ローカル体験 · ワールドの数値はサンプルです"
        WorldSyncStatus.CONNECTING -> "ワールドを確認中 · 集中はいつでも始められます"
        WorldSyncStatus.OFFLINE -> "オフライン · ワールド未取得または前回の情報です"
        WorldSyncStatus.LIVE -> "共有ワールド · 最終取得時の情報です"
    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
