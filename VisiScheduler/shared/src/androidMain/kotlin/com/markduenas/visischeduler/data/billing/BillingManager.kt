package com.markduenas.visischeduler.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.markduenas.visischeduler.config.AdMobConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manager for Google Play Billing integration.
 * Handles in-app purchases for removing ads.
 */
class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var currentActivity: Activity? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _hasRemovedAds = MutableStateFlow(false)
    val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    /**
     * Initialize the billing client and connect to Google Play.
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    // Query existing purchases
                    queryExistingPurchases()
                    // Query product details
                    queryProductDetails()
                } else {
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                // Retry connection
                startConnection()
            }
        })
    }

    private fun queryExistingPurchases() {
        billingClient?.let { client ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    handlePurchases(purchases)
                }
            }
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdMobConstants.REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
            }
        }
    }

    /**
     * Get the formatted price for ad removal.
     */
    suspend fun getAdRemovalPrice(): String? {
        if (productDetails == null) {
            // Try to fetch product details if not already loaded
            val result = fetchProductDetails()
            if (result == null) return null
        }

        return productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    private suspend fun fetchProductDetails(): ProductDetails? = suspendCancellableCoroutine { continuation ->
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(AdMobConstants.REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
                continuation.resume(productDetails)
            } else {
                continuation.resume(null)
            }
        } ?: continuation.resume(null)
    }

    /**
     * Launch the purchase flow for removing ads.
     */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        currentActivity = activity

        val details = productDetails ?: run {
            _purchaseState.value = PurchaseState.Error("Product not available")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

        return billingResult?.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Cancelled
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    "Purchase failed: ${billingResult.debugMessage}"
                )
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(AdMobConstants.REMOVE_ADS_PRODUCT_ID)) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Acknowledge the purchase if not already done
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        _hasRemovedAds.value = true
                        _purchaseState.value = PurchaseState.Purchased
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _hasRemovedAds.value = true
                _purchaseState.value = PurchaseState.Purchased
            } else {
                _purchaseState.value = PurchaseState.Error(
                    "Failed to acknowledge purchase: ${billingResult.debugMessage}"
                )
            }
        }
    }

    /**
     * Restore previous purchases.
     */
    suspend fun restorePurchases(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasRemoveAdsPurchase = purchases.any { purchase ->
                        purchase.products.contains(AdMobConstants.REMOVE_ADS_PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    _hasRemovedAds.value = hasRemoveAdsPurchase
                    continuation.resume(hasRemoveAdsPurchase)
                } else {
                    continuation.resume(false)
                }
            } ?: continuation.resume(false)
        }
    }

    /**
     * Clean up billing client resources.
     */
    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
    }
}

/**
 * Represents the current state of a purchase operation.
 */
sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Processing : PurchaseState()
    data object Purchased : PurchaseState()
    data object Cancelled : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}
