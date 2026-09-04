package com.madowaku.focusraid.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ProAccessRepository {
    val access: StateFlow<ProAccessState>
    val purchaseState: StateFlow<PurchaseState>

    suspend fun refresh()
    suspend fun purchasePro(activity: Activity)
    suspend fun restorePurchases()
}

class DefaultProAccessRepository(
    private val gateway: BillingGateway,
) : ProAccessRepository {
    private val _access = MutableStateFlow(ProAccessState())
    override val access: StateFlow<ProAccessState> = _access.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    override suspend fun refresh() {
        _access.value = _access.value.copy(refreshing = true, errorMessage = null)
        gateway.refresh()
            .onSuccess(::applySnapshot)
            .onFailure { error ->
                // Never demote a cached Pro user because a refresh failed.
                _access.value = _access.value.copy(
                    refreshing = false,
                    errorMessage = error.message ?: "課金情報を確認できませんでした",
                )
            }
    }

    override suspend fun purchasePro(activity: Activity) {
        _purchaseState.value = PurchaseState.Purchasing
        when (val result = gateway.purchasePro(activity)) {
            is BillingActionResult.Success -> {
                applySnapshot(result.snapshot)
                _purchaseState.value = if (result.snapshot.isPro) {
                    PurchaseState.Success
                } else {
                    PurchaseState.Error("購入は完了しましたがPro権限を確認できませんでした")
                }
            }

            BillingActionResult.Cancelled -> {
                _purchaseState.value = PurchaseState.Idle
            }

            is BillingActionResult.Failure -> {
                _purchaseState.value = PurchaseState.Error(result.message)
            }
        }
    }

    override suspend fun restorePurchases() {
        _purchaseState.value = PurchaseState.Restoring
        when (val result = gateway.restorePurchases()) {
            is BillingActionResult.Success -> {
                applySnapshot(result.snapshot)
                _purchaseState.value = PurchaseState.Success
            }

            BillingActionResult.Cancelled -> {
                _purchaseState.value = PurchaseState.Idle
            }

            is BillingActionResult.Failure -> {
                // Restore failures must not revoke an already-known Pro entitlement.
                _purchaseState.value = PurchaseState.Error(result.message)
            }
        }
    }

    private fun applySnapshot(snapshot: BillingSnapshot) {
        _access.value = ProAccessState(
            accessLevel = if (snapshot.isPro) AccessLevel.PRO else AccessLevel.FREE,
            product = snapshot.product ?: _access.value.product,
            refreshing = false,
            errorMessage = null,
        )
    }
}
