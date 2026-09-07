package com.madowaku.focusraid

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.madowaku.focusraid.billing.*
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingActionTest {
    @Test fun purchaseCancellationFailureAndSuccessAreRetryable() = runBlocking {
        var next: BillingActionResult = BillingActionResult.Cancelled
        val gateway = object : BillingGateway {
            override suspend fun refresh() = Result.success(BillingSnapshot(false))
            override suspend fun purchasePro(activity: Activity) = next
            override suspend fun restorePurchases() = next
        }
        val repository = DefaultProAccessRepository(gateway)
        val activity = withContext(Dispatchers.Main) { Activity() } // Fixture never opens Play.
        repository.purchasePro(activity)
        assertEquals(PurchaseState.Idle, repository.purchaseState.value)
        next = BillingActionResult.Failure("private billing SDK details")
        repository.purchasePro(activity)
        assertEquals(PurchaseState.Error(DefaultProAccessRepository.ACTION_ERROR), repository.purchaseState.value)
        assertEquals(AccessLevel.FREE, repository.access.value.accessLevel)
        next = BillingActionResult.Success(BillingSnapshot(true, verifiedAtMillis = 1))
        repository.purchasePro(activity)
        assertEquals(AccessLevel.PRO, repository.access.value.accessLevel)
        assertEquals(PurchaseState.Success, repository.purchaseState.value)
    }
}
