package com.madowaku.focusraid.billing

import android.app.Activity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProAccessRepositoryTest {
    @Test
    fun `refresh promotes access to Pro when entitlement is active`() = runBlocking {
        val gateway = FakeBillingGateway(
            Result.success(
                BillingSnapshot(
                    isPro = true,
                    product = ProProduct("focus_raid_pro_lifetime", "¥500"),
                ),
            ),
        )
        val repository = DefaultProAccessRepository(gateway)

        repository.refresh()

        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        assertEquals("¥500", repository.access.value.product?.formattedPrice)
        assertNull(repository.access.value.errorMessage)
    }

    @Test
    fun `failed refresh never demotes an already known Pro user`() = runBlocking {
        val gateway = FakeBillingGateway(Result.success(BillingSnapshot(isPro = true)))
        val repository = DefaultProAccessRepository(gateway)

        repository.refresh()
        gateway.nextRefresh = Result.failure(IllegalStateException("offline"))
        repository.refresh()

        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        assertEquals("offline", repository.access.value.errorMessage)
    }

    private class FakeBillingGateway(
        var nextRefresh: Result<BillingSnapshot>,
    ) : BillingGateway {
        override suspend fun refresh(): Result<BillingSnapshot> = nextRefresh

        override suspend fun purchasePro(activity: Activity): BillingActionResult =
            BillingActionResult.Failure("not used")

        override suspend fun restorePurchases(): BillingActionResult =
            BillingActionResult.Failure("not used")
    }
}
