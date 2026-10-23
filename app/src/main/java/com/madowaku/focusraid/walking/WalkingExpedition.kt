package com.madowaku.focusraid.walking

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WalkingDay(val steps: Long?, val checkpoints: Set<Int> = emptySet())

data class WalkingJournal(
    val enabled: Boolean = false,
    val firstDay: LocalDate? = null,
    val zone: String = "",
    val days: Map<LocalDate, WalkingDay> = emptyMap(),
) {
    fun enable(now: Instant, currentZone: ZoneId): WalkingJournal = copy(
        enabled = true,
        firstDay = firstDay ?: now.atZone(currentZone).toLocalDate(),
        zone = zone.ifEmpty { currentZone.id },
    )

    // Keep one calendar for this journal: travelling cannot overlap reward days.
    fun datesToRead(now: Instant): List<LocalDate> {
        val first = firstDay ?: return emptyList()
        val today = now.atZone(ZoneId.of(zone)).toLocalDate()
        val start = maxOf(first, today.minusDays(29))
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= today }.toList()
    }

    fun record(date: LocalDate, steps: Long?): WalkingJournal {
        require(steps == null || steps >= 0)
        val earned = days[date]?.checkpoints.orEmpty() + WalkingExpedition.thresholds.filter {
            steps != null && steps >= it
        }
        return copy(days = days + (date to WalkingDay(steps, earned)))
    }
}

data class WalkingDiscovery(val place: String, val story: String, val keepsake: String)

object WalkingExpedition {
    val thresholds = listOf(1_000, 3_000, 5_000)
    private val discoveries = listOf(
        WalkingDiscovery("森の小さな泉", "木漏れ日の下で、水面に小さな輪が広がった。相棒とひと休み。", "泉のしずく"),
        WalkingDiscovery("苔むした道標", "苔の奥に、まだ知らない道を指す矢印を見つけた。", "道標の写し"),
        WalkingDiscovery("星明かりの焚き火", "静かな火を囲んで、今日見つけた景色を思い返した。", "星色の小石"),
        WalkingDiscovery("小さな足跡", "柔らかな土に、誰かが通った跡。相棒もそっと隣に並んだ。", "足跡のスケッチ"),
        WalkingDiscovery("風の渡る丘", "草が波のように揺れる。遠くの空まで道が続いていた。", "風待ちの羽根"),
        WalkingDiscovery("月の花畑", "薄い花びらが夜を照らす。帰り道の目印をひとつ覚えた。", "月花のしおり"),
    )
    fun discovery(date: LocalDate, checkpoint: Int): WalkingDiscovery {
        require(checkpoint in thresholds)
        return discoveries[Math.floorMod(date.toEpochDay() * 3 + thresholds.indexOf(checkpoint), discoveries.size.toLong()).toInt()]
    }
}

enum class StepsAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

interface StepsRepository {
    fun availability(): StepsAvailability
    suspend fun hasPermission(): Boolean
    suspend fun read(start: Instant, end: Instant): Long?
}

/** Explicit test double; never selected by the production factory. */
class FakeStepsRepository(
    var permission: Boolean = true,
    var count: Long? = null,
    var status: StepsAvailability = StepsAvailability.AVAILABLE,
) : StepsRepository {
    override fun availability() = status
    override suspend fun hasPermission() = permission
    override suspend fun read(start: Instant, end: Instant): Long? = count
}
