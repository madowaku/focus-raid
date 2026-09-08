package com.madowaku.focusraid.billing

import android.app.Activity

interface BillingGateway {
    suspend fun refresh(): Result<BillingSnapshot>

    suspend fun purchasePro(activity: Activity): BillingActionResult

    suspend fun restorePurchases(): BillingActionResult
}
