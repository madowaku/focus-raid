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
        isConfigured = runCatching {
            Purchases.configure(PurchasesConfiguration.Builder(context.applicationContext, apiKey).build())
        }.isSuccess
        return isConfigured
    }
}

class RevenueCatBillingGateway(
    private val configured: () -> Boolean = { RevenueCatRuntime.isConfigured },
) : BillingGateway {
    override suspend fun refresh(): Result<BillingSnapshot> {
        if (!configured()) {
            return Result.failure(
                IllegalStateException("購入機能はまだ設定されていません"),
            )
        }

        return runCatching {
            val customerInfo = awaitCustomerInfo()
            val product = runCatching { awaitProPackage().toProProduct() }.getOrNull()
            BillingSnapshot(
                isPro = customerInfo.hasProEntitlement(),
                verifiedAtMillis = customerInfo.requestDate.time,
                product = product,
            )
        }
    }

    override suspend fun purchasePro(activity: Activity): BillingActionResult {
        if (!configured()) return unavailableFailure()

        val packageToPurchase = runCatching { kotlinx.coroutines.withTimeout(15_000) { awaitProPackage() } }
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
                                BillingActionResult.Failure(DefaultProAccessRepository.ACTION_ERROR)
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
                verifiedAtMillis = customerInfo.requestDate.time,
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
                        continuation.resume(BillingActionResult.Failure(DefaultProAccessRepository.ACTION_ERROR))
                    }
                },
                onSuccess = { customerInfo ->
                    if (continuation.isActive) {
                        continuation.resume(
                            BillingActionResult.Success(
                                BillingSnapshot(
                                    isPro = customerInfo.hasProEntitlement(),
                verifiedAtMillis = customerInfo.requestDate.time,
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
                        ?.firstOrNull {
                            it.product.id == BillingConfig.PRODUCT_ID &&
                                it.packageType == com.revenuecat.purchases.PackageType.LIFETIME &&
                                it.product.type == com.revenuecat.purchases.ProductType.INAPP
                        }

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

    private fun unavailableFailure(): BillingActionResult.Failure =
        BillingActionResult.Failure("購入機能はまだ設定されていません")

    private fun Throwable.userMessage(): String =
        DefaultProAccessRepository.REFRESH_ERROR
}
