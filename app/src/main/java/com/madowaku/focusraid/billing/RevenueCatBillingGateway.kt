package com.madowaku.focusraid.billing

import android.app.Activity
import android.content.Context
import com.madowaku.focusraid.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.Package as RevenueCatPackage
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object BillingConfig {
    const val ENTITLEMENT_ID = "pro"
    const val PRODUCT_ID = "focus_raid_pro_lifetime"
}

object RevenueCatRuntime {
    @Volatile
    var isConfigured: Boolean = false
        private set

    fun configure(context: Context): Boolean {
        if (isConfigured) return true

        val apiKey = BuildConfig.REVENUECAT_GOOGLE_API_KEY.trim()
        if (apiKey.isBlank()) return false

        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, apiKey).build(),
        )
        isConfigured = true
        return true
    }
}

class RevenueCatBillingGateway(
    private val configured: () -> Boolean = { RevenueCatRuntime.isConfigured },
) : BillingGateway {
    override suspend fun refresh(): Result<BillingSnapshot> = runCatching {
        ensureConfigured()
        val customerInfo = awaitCustomerInfo()
        val product = runCatching { awaitProPackage().toProProduct() }.getOrNull()
        BillingSnapshot(
            isPro = customerInfo.hasProEntitlement(),
            product = product,
        )
    }

    override suspend fun purchasePro(activity: Activity): BillingActionResult {
        if (!configured()) return unavailableFailure()

        val packageToPurchase = runCatching { awaitProPackage() }
            .getOrElse { return BillingActionResult.Failure(it.userMessage()) }

        val params = PurchaseParams.Builder(activity, packageToPurchase).build()
        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchaseWith(
                purchaseParams = params,
                onError = { error, userCancelled ->
                    if (continuation.isActive) {
                        continuation.resume(
                            if (userCancelled) {
                                BillingActionResult.Cancelled
                            } else {
                                BillingActionResult.Failure(error.message)
                            },
                        )
                    }
                },
                onSuccess = { _, customerInfo ->
                    if (continuation.isActive) {
                        continuation.resume(
                            BillingActionResult.Success(
                                BillingSnapshot(
                                    isPro = customerInfo.hasProEntitlement(),
                                    product = packageToPurchase.toProProduct(),
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    override suspend fun restorePurchases(): BillingActionResult {
        if (!configured()) return unavailableFailure()

        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resume(BillingActionResult.Failure(error.message))
                    }
                },
                onSuccess = { customerInfo ->
                    if (continuation.isActive) {
                        continuation.resume(
                            BillingActionResult.Success(
                                BillingSnapshot(
                                    isPro = customerInfo.hasProEntitlement(),
                                    product = null,
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    private suspend fun awaitCustomerInfo(): CustomerInfo =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(IllegalStateException(error.message)))
                    }
                },
                onSuccess = { customerInfo ->
                    if (continuation.isActive) continuation.resume(customerInfo)
                },
            )
        }

    private suspend fun awaitProPackage(): RevenueCatPackage =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(IllegalStateException(error.message)))
                    }
                },
                onSuccess = { offerings ->
                    val current = offerings.current
                    val packageToPurchase = current?.availablePackages
                        ?.firstOrNull { it.product.id == BillingConfig.PRODUCT_ID }
                        ?: current?.lifetime

                    if (!continuation.isActive) return@getOfferingsWith
                    if (packageToPurchase == null) {
                        continuation.resumeWith(
                            Result.failure(
                                IllegalStateException("Focus Raid Proの商品情報が見つかりません"),
                            ),
                        )
                    } else {
                        continuation.resume(packageToPurchase)
                    }
                },
            )
        }

    private fun RevenueCatPackage.toProProduct(): ProProduct = ProProduct(
        productId = product.id,
        formattedPrice = product.price.formatted,
    )

    private fun CustomerInfo.hasProEntitlement(): Boolean =
        entitlements[BillingConfig.ENTITLEMENT_ID]?.isActive == true

    private fun ensureConfigured() {
        check(configured()) { "RevenueCat API key is not configured" }
    }

    private fun unavailableFailure(): BillingActionResult.Failure =
        BillingActionResult.Failure("購入機能はまだ設定されていません")

    private fun Throwable.userMessage(): String =
        message ?: "商品情報を取得できませんでした"
}
