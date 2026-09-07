@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.madowaku.focusraid.billing

import android.app.Activity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.Assert.*

class BillingRecoveryTest {
    private class Gateway : BillingGateway {
        var refresh: suspend () -> Result<BillingSnapshot> = { Result.success(BillingSnapshot(false)) }
        var action: suspend () -> BillingActionResult = { BillingActionResult.Cancelled }
        override suspend fun refresh() = refresh.invoke()
        override suspend fun purchasePro(activity: Activity) = action()
        override suspend fun restorePurchases() = action()
    }
    private class Cache(var value: BillingSnapshot? = null) : ProAccessCache {
        override fun read() = value
        override suspend fun write(snapshot: BillingSnapshot) { value = snapshot }
    }

    @Test fun `durable cache writes cannot reorder Free after newer Pro`() = runTest {
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var saved: BillingSnapshot? = null
        val cache = object : ProAccessCache {
            override fun read() = saved
            override suspend fun write(snapshot: BillingSnapshot) {
                if (!snapshot.isPro) { started.complete(Unit); release.await() }
                saved = snapshot
            }
        }
        val gateway = Gateway().apply {
            refresh = { Result.success(BillingSnapshot(false, verifiedAtMillis = 100)) }
            action = { BillingActionResult.Success(BillingSnapshot(true, verifiedAtMillis = 200)) }
        }
        val repository = DefaultProAccessRepository(gateway, cache)
        val refresh = launch { repository.refresh() }; started.await()
        val restore = launch { repository.restorePurchases() }; runCurrent()
        release.complete(Unit); refresh.join(); restore.join()
        assertEquals(AccessLevel.PRO, DefaultProAccessRepository(gateway, cache).access.value.accessLevel)
    }

    @Test fun `older refresh cannot revoke successful restore`() = runTest {
        val gate = CompletableDeferred<Result<BillingSnapshot>>()
        val gateway = Gateway().apply {
            refresh = { gate.await() }
            action = { BillingActionResult.Success(BillingSnapshot(true, verifiedAtMillis = 200)) }
        }
        val repository = DefaultProAccessRepository(gateway)
        val refresh = launch { repository.refresh() }; runCurrent()
        repository.restorePurchases()
        gate.complete(Result.success(BillingSnapshot(false, verifiedAtMillis = 100))); refresh.join()
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        assertEquals(PurchaseState.Success, repository.purchaseState.value)
    }
    @Test fun `verified Pro survives restart and offline refresh but a newer refund can revoke`() = runTest {
        val cache = Cache(BillingSnapshot(true, verifiedAtMillis = 100))
        val gateway = Gateway().apply { refresh = { Result.failure(Exception("sdk private text")) } }
        val repository = DefaultProAccessRepository(gateway, cache)
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        repository.refresh()
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        assertFalse(repository.access.value.errorMessage!!.contains("sdk"))
        gateway.refresh = { Result.success(BillingSnapshot(false, verifiedAtMillis = 100)) }
        repository.refresh()
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        gateway.refresh = { Result.success(BillingSnapshot(false, verifiedAtMillis = 101)) }
        repository.refresh()
        assertEquals(AccessLevel.FREE, repository.access.value.accessLevel)
        assertFalse(cache.value!!.isPro)
    }
    @Test fun `cancel and error restore preserve Pro and remain retryable`() = runTest {
        val gateway = Gateway()
        val repository = DefaultProAccessRepository(gateway, Cache(BillingSnapshot(true)))
        repository.restorePurchases()
        assertEquals(PurchaseState.Idle, repository.purchaseState.value)
        gateway.action = { BillingActionResult.Failure("raw exception") }
        repository.restorePurchases()
        assertEquals(PurchaseState.Error(DefaultProAccessRepository.ACTION_ERROR), repository.purchaseState.value)
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        gateway.action = { BillingActionResult.Success(BillingSnapshot(true)) }
        repository.restorePurchases()
        assertEquals(PurchaseState.Success, repository.purchaseState.value)
    }
    @Test fun `duplicate restore taps do not dispatch multiple billing operations`() = runTest {
        val gate = CompletableDeferred<BillingActionResult>(); var count = 0
        val gateway = Gateway().apply { action = { count++; gate.await() } }
        val repository = DefaultProAccessRepository(gateway)
        val first = launch { repository.restorePurchases() }; runCurrent()
        repository.restorePurchases(); repository.clearPurchaseState()
        assertEquals(1, count)
        assertEquals(PurchaseState.Restoring, repository.purchaseState.value)
        gate.complete(BillingActionResult.Cancelled); first.join()
        assertEquals(PurchaseState.Idle, repository.purchaseState.value)
    }
    @Test fun `missing configuration keeps Free and retry loads pricing`() = runTest {
        val gateway = Gateway().apply { refresh = { Result.failure(Exception("config missing")) } }
        val repository = DefaultProAccessRepository(gateway)
        repository.refresh()
        assertEquals(AccessLevel.FREE, repository.access.value.accessLevel)
        assertNull(repository.access.value.product)
        gateway.refresh = { Result.success(BillingSnapshot(false, ProProduct("focus_raid_pro_lifetime", "¥500"))) }
        repository.refresh()
        assertEquals("¥500", repository.access.value.product?.formattedPrice)
        assertNull(repository.access.value.errorMessage)
    }
}
