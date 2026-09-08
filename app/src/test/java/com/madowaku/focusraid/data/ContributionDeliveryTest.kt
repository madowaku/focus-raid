@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.madowaku.focusraid.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Assert.*

class ContributionDeliveryTest {
    private fun row(id: String = "session") = WorldContribution(id, "N", 25, 25, 2_000_000, startedAtEpochMillis = 500_000)

    @Test fun responseLossAndRestartReplayCountExactlyOnce() = runTest {
        val dao = MemoryContributionDao(); dao.insert(row())
        val accepted = mutableSetOf<String>(); var loseResponse = true; var damage = 0
        val gateway = ContributionGateway {
            val first = accepted.add(it.sessionId)
            if (first) damage += it.creditedMinutes
            if (loseResponse) { loseResponse = false; error("lost response") }
            ContributionReceipt(if (first) ContributionStatus.ACCEPTED else ContributionStatus.ALREADY_COUNTED, 25)
        }
        assertTrue(ContributionDelivery(dao, gateway).drain())
        assertEquals(ContributionStatus.OFFLINE, dao.rows.value.single().state)
        assertFalse(ContributionDelivery(dao, gateway).drain())
        assertEquals(25, damage)
        assertEquals(ContributionStatus.ALREADY_COUNTED, dao.rows.value.single().state)
    }

    @Test fun cancellationLeavesRetryableRowAndTerminalReceiptCannotBeDowngraded() = runTest {
        val dao = MemoryContributionDao(); dao.insert(row())
        val delivery = ContributionDelivery(dao) { awaitCancellation() }
        val job = launch { delivery.drain() }; runCurrent(); job.cancelAndJoin()
        assertEquals(ContributionStatus.RETRYING, dao.rows.value.single().state)
        ContributionDelivery(dao) { ContributionReceipt(ContributionStatus.ACCEPTED, 25) }.drain()
        dao.resolve("session", "OFFLINE")
        assertEquals(ContributionStatus.ACCEPTED, dao.rows.value.single().state)
    }

    @Test fun timeoutAuthAndRejectionAreDistinctAndStaleStopsRetrying() = runTest {
        val dao = MemoryContributionDao(); dao.insert(row())
        assertTrue(ContributionDelivery(dao) { delay(20_000); error("late") }.drain())
        assertEquals(ContributionStatus.OFFLINE, dao.rows.value.single().state)
        assertTrue(ContributionDelivery(dao) { throw ContributionAuthException() }.drain())
        assertEquals(ContributionStatus.AUTH_REQUIRED, dao.rows.value.single().state)
        assertFalse(ContributionDelivery(dao) { ContributionReceipt(ContributionStatus.STALE, 0) }.drain())
        assertEquals(ContributionStatus.STALE, dao.rows.value.single().state)
        dao.insert(row("bad"))
        assertFalse(ContributionDelivery(dao) { throw ContributionRejectedException() }.drain())
        assertEquals(ContributionStatus.REJECTED, dao.rows.value.last().state)
    }

    @Test fun boundedDrainContinuesPastFiftyAndNullGenerationIsRetainedForServerResolution() = runTest {
        val dao = MemoryContributionDao()
        repeat(51) { dao.insert(row("$it").copy(generation = null)) }
        val gateway = ContributionGateway {
            assertNull(it.generation)
            ContributionReceipt(ContributionStatus.ACCEPTED, 25)
        }
        assertTrue(ContributionDelivery(dao, gateway).drain())
        assertEquals(50, dao.rows.value.count { it.state == ContributionStatus.ACCEPTED })
        assertFalse(ContributionDelivery(dao, gateway).drain())
        assertEquals(51, dao.rows.value.count { it.state == ContributionStatus.ACCEPTED })
    }

    @Test fun concurrentDrainsDoNotDowngradeSuccessAfterLateFailure() = runTest {
        val dao = MemoryContributionDao(); dao.insert(row())
        val release = CompletableDeferred<Unit>()
        val slow = launch { ContributionDelivery(dao) { release.await(); error("timeout after other worker") }.drain() }
        runCurrent()
        ContributionDelivery(dao) { ContributionReceipt(ContributionStatus.ACCEPTED, 25) }.drain()
        release.complete(Unit); slow.join()
        assertEquals(ContributionStatus.ACCEPTED, dao.rows.value.single().state)
    }
}

private class MemoryContributionDao : ContributionDao {
    val rows = MutableStateFlow<List<WorldContribution>>(emptyList())
    override suspend fun insert(row: WorldContribution) { if (rows.value.none { it.sessionId == row.sessionId }) rows.value += row }
    override fun observeAll() = rows
    override suspend fun pending() = rows.value.filter { it.status in listOf("PENDING", "RETRYING", "OFFLINE", "AUTH_REQUIRED") }.take(50)
    override suspend fun resolve(id: String, status: String, damage: Int) {
        rows.value = rows.value.map { if (it.sessionId == id && it.status in listOf("PENDING", "RETRYING", "OFFLINE", "AUTH_REQUIRED")) it.copy(status = status, appliedDamage = damage) else it }
    }
}
