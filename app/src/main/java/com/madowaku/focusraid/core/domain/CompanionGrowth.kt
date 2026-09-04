package com.madowaku.focusraid.core.domain

enum class CompanionStage(
    val label: String,
    val startMinutes: Int,
    val nextThresholdMinutes: Int?,
    val nextLabel: String?,
) {
    EGG("卵", 0, 75, "幼体"),
    HATCHLING("幼体", 75, 720, "第一成長"),
    FIRST_GROWTH("第一成長", 720, 1_800, "第二成長"),
    SECOND_GROWTH("第二成長", 1_800, 4_500, "成熟"),
    MATURE("成熟", 4_500, null, null),
}

data class CompanionGrowthStatus(
    val stage: CompanionStage,
    val totalMinutes: Int,
    val progress: Float,
    val remainingMinutes: Int,
) {
    val nextThresholdMinutes: Int?
        get() = stage.nextThresholdMinutes

    val nextStageLabel: String?
        get() = stage.nextLabel
}

data class CompanionEvolution(
    val from: CompanionStage,
    val to: CompanionStage,
)

object CompanionGrowth {
    fun from(totalMinutes: Int): CompanionGrowthStatus {
        val safeTotal = totalMinutes.coerceAtLeast(0)
        val stage = when {
            safeTotal < 75 -> CompanionStage.EGG
            safeTotal < 720 -> CompanionStage.HATCHLING
            safeTotal < 1_800 -> CompanionStage.FIRST_GROWTH
            safeTotal < 4_500 -> CompanionStage.SECOND_GROWTH
            else -> CompanionStage.MATURE
        }

        val nextThreshold = stage.nextThresholdMinutes
        if (nextThreshold == null) {
            return CompanionGrowthStatus(
                stage = stage,
                totalMinutes = safeTotal,
                progress = 1f,
                remainingMinutes = 0,
            )
        }

        val span = (nextThreshold - stage.startMinutes).coerceAtLeast(1)
        val stageMinutes = (safeTotal - stage.startMinutes).coerceIn(0, span)
        return CompanionGrowthStatus(
            stage = stage,
            totalMinutes = safeTotal,
            progress = stageMinutes.toFloat() / span.toFloat(),
            remainingMinutes = (nextThreshold - safeTotal).coerceAtLeast(0),
        )
    }

    fun evolutionBetween(beforeMinutes: Int, afterMinutes: Int): CompanionEvolution? {
        val before = from(beforeMinutes).stage
        val after = from(afterMinutes).stage
        return if (after.ordinal > before.ordinal) {
            CompanionEvolution(from = before, to = after)
        } else {
            null
        }
    }
}
