package com.madowaku.focusraid.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProAccessViewModel(
    private val repository: ProAccessRepository,
) : ViewModel() {
    val access: StateFlow<ProAccessState> = repository.access
    val purchaseState: StateFlow<PurchaseState> = repository.purchaseState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun purchasePro(activity: Activity) {
        viewModelScope.launch { repository.purchasePro(activity) }
    }

    fun restorePurchases() {
        viewModelScope.launch { repository.restorePurchases() }
    }

    fun clearPurchaseState() {
        repository.clearPurchaseState()
    }

    class Factory(
        private val repository: ProAccessRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProAccessViewModel(repository) as T
    }
}
