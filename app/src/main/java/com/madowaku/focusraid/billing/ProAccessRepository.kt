package com.madowaku.focusraid.billing

import kotlinx.coroutines.sync.withLock
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
    fun clearPurchaseState()
}

class DefaultProAccessRepository(
    private val gateway: BillingGateway,
    private val cache: ProAccessCache? = null,
) : ProAccessRepository {
    private val cached = cache?.read()
    private var verifiedAt = cached?.verifiedAtMillis ?: 0L
    private var generation = 0L
    private val snapshotLock = kotlinx.coroutines.sync.Mutex()
    private val actionLock = kotlinx.coroutines.sync.Mutex()
    private val _access = MutableStateFlow(ProAccessState(
        accessLevel = if (cached?.isPro == true) AccessLevel.PRO else AccessLevel.FREE,
    ))
    override val access: StateFlow<ProAccessState> = _access.asStateFlow()
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    override suspend fun refresh() {
        if (actionLock.isLocked) return
        val request = ++generation
        _access.value = _access.value.copy(refreshing = true, errorMessage = null)
        val result = try {
            kotlinx.coroutines.withTimeout(15_000) { gateway.refresh() }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) { Result.failure(e)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
        if (request != generation) return
        result.fold(
            onSuccess = { applySnapshot(it) },
            onFailure = { _access.value = _access.value.copy(refreshing = false, errorMessage = REFRESH_ERROR) },
        )
    }

    override suspend fun purchasePro(activity: Activity) = action { gateway.purchasePro(activity) }
    override suspend fun restorePurchases() = action(restoring = true) { gateway.restorePurchases() }

    private suspend fun action(restoring: Boolean = false, block: suspend () -> BillingActionResult) {
        if (!actionLock.tryLock()) return
        ++generation
        _access.value = _access.value.copy(refreshing = false)
        _purchaseState.value = if (restoring) PurchaseState.Restoring else PurchaseState.Purchasing
        try {
            val result = if (restoring) kotlinx.coroutines.withTimeout(30_000) { block() } else block()
            when (result) {
                is BillingActionResult.Success -> {
                    applySnapshot(result.snapshot)
                    _purchaseState.value = if (restoring || result.snapshot.isPro) PurchaseState.Success
                        else PurchaseState.Error("購入の反映を確認できませんでした。購入を復元して再確認できます。")
                }
                BillingActionResult.Cancelled -> _purchaseState.value = PurchaseState.Idle
                is BillingActionResult.Failure -> _purchaseState.value = PurchaseState.Error(ACTION_ERROR)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            _purchaseState.value = PurchaseState.Error(ACTION_ERROR)
        } catch (e: kotlinx.coroutines.CancellationException) {
            _purchaseState.value = PurchaseState.Idle
            throw e
        } catch (_: Exception) {
            _purchaseState.value = PurchaseState.Error(ACTION_ERROR)
        } finally { actionLock.unlock() }
    }

    override fun clearPurchaseState() {
        if (!actionLock.isLocked) _purchaseState.value = PurchaseState.Idle
    }

    private suspend fun applySnapshot(snapshot: BillingSnapshot) = snapshotLock.withLock {
        // Undated/cached negatives cannot revoke known Pro. Newer server info can revoke refunds.
        val stale = snapshot.verifiedAtMillis < verifiedAt ||
            (!snapshot.isPro && _access.value.accessLevel == AccessLevel.PRO && snapshot.verifiedAtMillis <= verifiedAt)
        if (!stale) {
            verifiedAt = snapshot.verifiedAtMillis
            _access.value = _access.value.copy(accessLevel = if (snapshot.isPro) AccessLevel.PRO else AccessLevel.FREE)
            try { cache?.write(snapshot) } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (_: Exception) { /* Retain live access; SDK also caches CustomerInfo. */ }
        }
        _access.value = _access.value.copy(product = snapshot.product ?: _access.value.product,
            refreshing = false, errorMessage = if (snapshot.product == null) REFRESH_ERROR else null)
    }

    companion object {
        const val REFRESH_ERROR = "購入情報を確認できませんでした。通信を確認して再試行してください。Freeはそのまま使えます。"
        const val ACTION_ERROR = "購入を確認できませんでした。通信とGoogle Playのアカウントを確認して再試行してください。"
    }
}
