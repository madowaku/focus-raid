package com.madowaku.focusraid.ui

import com.madowaku.focusraid.data.RaidEcho
import kotlin.math.max
import kotlin.math.min

internal enum class ReturnRaidPhase {
    RETURNING,
    ECHO,
    YOUR_TURN,
    STRIKING,
    RESULT,
    CAMP,
}

internal data class RaidEchoUi(
    val relativeTime: String,
    val focusMinutes: Int,
    val damage: Int,
    val displayDamage: Int,
)

internal data class ReturnRaidUiState(
    val phase: ReturnRaidPhase,
    val echoIndex: Int,
    val displayedHp: Int,
    val strikeCommitted: Boolean,
    val armorBroken: Boolean,
    val chainCount: Int,
    val chainMinutes: Int,
)

internal data class ReturnRaidScenario(
    val bossName: String,
    val bossMaxHp: Int,
    val presentBossHp: Int,
    val playerDamage: Int,
    val creditedMinutes: Int,
    val echoes: List<RaidEchoUi>,
    val chainCountBefore: Int,
    val chainMinutesBefore: Int,
) {
    val initialDisplayedHp: Int
        get() = min(
            bossMaxHp,
            presentBossHp + echoes.sumOf { it.displayDamage.coerceAtLeast(0) },
        )

    val hpAfterPlayerStrike: Int
        get() = max(0, presentBossHp - playerDamage.coerceAtLeast(0))

    private val totalEchoDamage: Int
        get() = echoes.sumOf { it.displayDamage.coerceAtLeast(0) }

    private val visibleEchoDamage: Int
        get() = (initialDisplayedHp - presentBossHp).coerceAtLeast(0)

    val armorBreaks: Boolean
        get() = presentBossHp > 0 && hpAfterPlayerStrike == 0

    val chainCountAfter: Int
        get() = chainCountBefore + if (creditedMinutes > 0) 1 else 0

    val chainMinutesAfter: Int
        get() = chainMinutesBefore + creditedMinutes.coerceAtLeast(0)

    fun hpAfterEcho(index: Int): Int {
        if (echoes.isEmpty()) return presentBossHp.coerceIn(0, bossMaxHp)
        val safeIndex = index.coerceIn(0, echoes.lastIndex)
        val remainingEchoDamage = echoes
            .drop(safeIndex + 1)
            .sumOf { it.displayDamage.coerceAtLeast(0) }
        if (totalEchoDamage == 0 || visibleEchoDamage == 0) {
            return presentBossHp.coerceIn(0, bossMaxHp)
        }

        // When the reconstructed pre-echo HP would exceed max HP, preserve a visible
        // step for each real echo by distributing the available HP range proportionally.
        val remainingVisibleDamage = visibleEchoDamage.toLong() * remainingEchoDamage / totalEchoDamage
        return (presentBossHp + remainingVisibleDamage.toInt()).coerceIn(0, bossMaxHp)
    }

    companion object {
        fun fromEchoes(
            bossName: String,
            bossHp: Int,
            bossMaxHp: Int,
            playerDamage: Int,
            creditedMinutes: Int,
            echoes: List<RaidEcho>,
            chainCountBefore: Int,
            chainMinutesBefore: Int,
        ): ReturnRaidScenario {
            val safeMax = bossMaxHp.coerceAtLeast(1)
            val safePresent = bossHp.coerceIn(0, safeMax)
            return ReturnRaidScenario(
                bossName = bossName,
                bossMaxHp = safeMax,
                presentBossHp = safePresent,
                playerDamage = playerDamage.coerceAtLeast(0),
                creditedMinutes = creditedMinutes.coerceAtLeast(0),
                // At most two real echoes, followed by the player's light.
                echoes = echoes.take(2).map { echo ->
                    RaidEchoUi(
                        relativeTime = echo.relativeLabel,
                        focusMinutes = echo.focusMinutes.coerceAtLeast(0),
                        damage = echo.damage.coerceAtLeast(0),
                        displayDamage = echo.damage.coerceAtLeast(0),
                    )
                },
                chainCountBefore = chainCountBefore.coerceAtLeast(0),
                chainMinutesBefore = chainMinutesBefore.coerceAtLeast(0),
            )
        }

        fun demo(
            bossName: String,
            bossHp: Int,
            bossMaxHp: Int,
            playerDamage: Int,
            creditedMinutes: Int,
        ): ReturnRaidScenario = fromEchoes(
            bossName = bossName,
            bossHp = bossHp,
            bossMaxHp = bossMaxHp,
            playerDamage = playerDamage,
            creditedMinutes = creditedMinutes,
            echoes = listOf(
                RaidEcho("3時間前", 25, 25),
                RaidEcho("51分前", 50, 50),
                RaidEcho("12分前", 25, 25),
            ),
            chainCountBefore = 6,
            chainMinutesBefore = 175,
        )
    }
}

internal class ReturnRaidSequenceStateMachine(
    private val scenario: ReturnRaidScenario,
) {
    var state: ReturnRaidUiState = ReturnRaidUiState(
        phase = ReturnRaidPhase.RETURNING,
        echoIndex = -1,
        displayedHp = scenario.initialDisplayedHp,
        strikeCommitted = false,
        armorBroken = false,
        chainCount = scenario.chainCountBefore,
        chainMinutes = scenario.chainMinutesBefore,
    )
        private set

    fun advance(): ReturnRaidUiState {
        state = when (state.phase) {
            ReturnRaidPhase.RETURNING -> {
                if (scenario.echoes.isEmpty()) {
                    state.copy(phase = ReturnRaidPhase.YOUR_TURN, displayedHp = scenario.presentBossHp)
                } else {
                    state.copy(
                        phase = ReturnRaidPhase.ECHO,
                        echoIndex = 0,
                        displayedHp = scenario.hpAfterEcho(0),
                    )
                }
            }

            ReturnRaidPhase.ECHO -> {
                val nextIndex = state.echoIndex + 1
                if (nextIndex <= scenario.echoes.lastIndex) {
                    state.copy(
                        echoIndex = nextIndex,
                        displayedHp = scenario.hpAfterEcho(nextIndex),
                    )
                } else {
                    state.copy(
                        phase = ReturnRaidPhase.YOUR_TURN,
                        displayedHp = scenario.presentBossHp,
                    )
                }
            }

            ReturnRaidPhase.STRIKING -> state.copy(
                phase = ReturnRaidPhase.RESULT,
                displayedHp = scenario.hpAfterPlayerStrike,
                armorBroken = scenario.armorBreaks,
            )

            ReturnRaidPhase.RESULT -> state.copy(
                phase = ReturnRaidPhase.CAMP,
                chainCount = scenario.chainCountAfter,
                chainMinutes = scenario.chainMinutesAfter,
            )

            ReturnRaidPhase.YOUR_TURN,
            ReturnRaidPhase.CAMP,
            -> state
        }
        return state
    }

    fun strike(): ReturnRaidUiState {
        if (state.phase != ReturnRaidPhase.YOUR_TURN || state.strikeCommitted) return state
        state = state.copy(
            phase = ReturnRaidPhase.STRIKING,
            strikeCommitted = true,
            displayedHp = scenario.hpAfterPlayerStrike,
        )
        return state
    }
}

internal fun reservesFirst25Moment(totalFocusMinutes: Int, creditedMinutes: Int): Boolean {
    val after = totalFocusMinutes.coerceAtLeast(0)
    val before = (after - creditedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
    return before < 25 && after >= 25
}
