package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.*
import org.junit.Assert.*
import org.junit.Test

class SharedRaidPresentationTest {
    private fun row(id: String, status: ContributionStatus, generation: String? = "raid-a") =
        WorldContribution(id, generation, 25, 25, 0, status.name)

    @Test fun `only unique acknowledged minutes from this generation count`() {
        val accepted = row("a", ContributionStatus.ACCEPTED)
        val rows = listOf(row("a", ContributionStatus.PENDING), accepted, accepted, row("b", ContributionStatus.ALREADY_COUNTED),
            row("c", ContributionStatus.PENDING), row("d", ContributionStatus.OFFLINE),
            row("e", ContributionStatus.ACCEPTED, "old"), row("f", ContributionStatus.STALE))
        assertEquals(50L, acceptedRaidMinutes("raid-a", rows))
        assertEquals(0L, acceptedRaidMinutes(null, rows))
    }
    @Test fun `offline and preview do not claim cached participation is live`() {
        val world = WorldSnapshot(generation = "raid-a", totalFocusMinutes = 9876, raidParticipants = 123)
        assertTrue(sharedRaidSummary(world, WorldSyncStatus.LIVE).contains("9876分"))
        for (status in listOf(WorldSyncStatus.OFFLINE, WorldSyncStatus.LOCAL_PREVIEW, WorldSyncStatus.CONNECTING)) {
            assertFalse(sharedRaidSummary(world, status).contains("9876"))
            assertFalse(sharedRaidSummary(world, status).contains("123"))
        }
    }
    @Test fun `replay ends at server snapshot without subtracting self twice`() {
        val scenario = ReturnRaidScenario.fromSharedSnapshot(
            WorldSnapshot(generation = "raid-a", bossHp = 400, bossMaxHp = 650),
            25, 25, listOf(RaidEcho("さっき", 50, 50, "raid-a")))
        assertEquals(475, scenario.initialDisplayedHp)
        assertEquals(425, scenario.presentBossHp)
        assertEquals(400, scenario.hpAfterPlayerStrike)
        assertEquals(75, scenario.chainMinutesAfter)
    }
    @Test fun `zero accepted damage cannot claim a finishing strike`() {
        val scenario = ReturnRaidScenario.fromSharedSnapshot(WorldSnapshot(bossHp = 0, bossMaxHp = 650), 0, 25, emptyList())
        assertEquals(0, scenario.hpAfterPlayerStrike)
        assertFalse(scenario.armorBreaks)
    }
    @Test fun `another generation or unknown generation cannot merge into this raid`() {
        val scenario = ReturnRaidScenario.fromSharedSnapshot(
            WorldSnapshot(generation = "raid-a", bossHp = 400, bossMaxHp = 650), 25, 25,
            listOf(RaidEcho("前回", 50, 50, "old"), RaidEcho("不明", 50, 50)))
        assertTrue(scenario.echoes.isEmpty())
        assertEquals(25, scenario.chainMinutesAfter)
        val unknownRaid = ReturnRaidScenario.fromSharedSnapshot(
            WorldSnapshot(bossHp = 400, bossMaxHp = 650), 25, 25,
            listOf(RaidEcho("不明", 50, 50)))
        assertTrue(unknownRaid.echoes.isEmpty())
    }
}
