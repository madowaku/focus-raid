package com.madowaku.focusraid.ui
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.ContributionStatus
import com.madowaku.focusraid.data.WorldContribution
import com.madowaku.focusraid.data.WorldSyncStatus

internal fun acceptedRaidMinutes(generation: String?, rows: List<WorldContribution>): Long {
    if (generation == null) return 0
    return rows.filter {
        it.generation == generation && it.state in setOf(ContributionStatus.ACCEPTED, ContributionStatus.ALREADY_COUNTED)
    }.distinctBy { it.sessionId }.sumOf { it.creditedMinutes.coerceAtLeast(0).toLong() }
}
internal fun sharedRaidSummary(world: WorldSnapshot, status: WorldSyncStatus): String = when {
    status == WorldSyncStatus.LOCAL_PREVIEW -> "体験プレビュー · 共有レイドへの送信はありません"
    status == WorldSyncStatus.CONNECTING -> "みんなの集中を確認中…"
    status == WorldSyncStatus.OFFLINE -> "オフライン · 共有レイドの最新状況は未確認"
    world.generation == null -> "共有レイドを準備中"
    else -> "みんなの集中 ${world.totalFocusMinutes.coerceAtLeast(0)}分 · ${world.raidParticipants.coerceAtLeast(0)}人が参加"
}
