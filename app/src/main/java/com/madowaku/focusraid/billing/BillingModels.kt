package com.madowaku.focusraid.billing

data class ProProduct(
    val productId: String,
    val formattedPrice: String,
)

data class BillingSnapshot(
    val isPro: Boolean,
    val product: ProProduct? = null,
)

sealed interface BillingActionResult {
    data class Success(val snapshot: BillingSnapshot) : BillingActionResult
    data object Cancelled : BillingActionResult
    data class Failure(val message: String) : BillingActionResult
}

enum class AccessLevel {
    FREE,
    PRO,
}

data class ProAccessState(
    val accessLevel: AccessLevel = AccessLevel.FREE,
    val product: ProProduct? = null,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PurchaseState {
    data object Idle : PurchaseState
    data object Purchasing : PurchaseState
    data object Restoring : PurchaseState
    data object Success : PurchaseState
    data class Error(val message: String) : PurchaseState
}
