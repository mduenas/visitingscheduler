package com.markduenas.visischeduler.data.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionState
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.StoreKit.SKPayment
import platform.darwin.NSObject

/** Constants for StoreKit */
private const val REMOVE_ADS_PRODUCT_ID = "com.markduenas.visischeduler.remove_ads"
private const val PREFS_KEY_ADS_REMOVED = "ads_removed"

/** Singleton holder for StoreKitHelper */
object StoreKitHelperProvider {
    val shared: StoreKitHelper by lazy { StoreKitHelper() }
}

/**
 * iOS StoreKit helper for in-app purchases.
 * Uses StoreKit 1 for broader compatibility.
 */
class StoreKitHelper : NSObject(), SKPaymentTransactionObserverProtocol, SKProductsRequestDelegateProtocol {

    private val _hasRemovedAds = MutableStateFlow(loadAdsRemovedState())
    val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    private val _purchaseState = MutableStateFlow<IAPPurchaseState>(IAPPurchaseState.Idle)
    val purchaseState: StateFlow<IAPPurchaseState> = _purchaseState.asStateFlow()

    private var product: SKProduct? = null
    private var productsRequest: SKProductsRequest? = null

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(this)
        loadProducts()
    }

    /**
     * Load product information from App Store.
     */
    fun loadProducts() {
        val productIds = setOf(REMOVE_ADS_PRODUCT_ID)
        productsRequest = SKProductsRequest(productIds)
        productsRequest?.delegate = this
        productsRequest?.start()
    }

    /**
     * Get formatted price for ad removal.
     */
    fun getFormattedPrice(): String? {
        val prod = product ?: return null
        val formatter = platform.Foundation.NSNumberFormatter()
        formatter.formatterBehavior = platform.Foundation.NSNumberFormatterBehavior10_4
        formatter.numberStyle = platform.Foundation.NSNumberFormatterCurrencyStyle
        formatter.locale = prod.priceLocale
        return formatter.stringFromNumber(prod.price)
    }

    /**
     * Check if purchases can be made.
     */
    fun canMakePurchases(): Boolean {
        return SKPaymentQueue.canMakePayments()
    }

    /**
     * Purchase ad removal.
     */
    fun purchaseAdRemoval(): Boolean {
        val prod = product ?: run {
            _purchaseState.value = IAPPurchaseState.Error("Product not available")
            return false
        }

        if (!canMakePurchases()) {
            _purchaseState.value = IAPPurchaseState.Error("Purchases are disabled")
            return false
        }

        _purchaseState.value = IAPPurchaseState.Purchasing
        val payment = SKPayment.paymentWithProduct(prod)
        SKPaymentQueue.defaultQueue().addPayment(payment)
        return true
    }

    /**
     * Restore previous purchases.
     */
    fun restorePurchases() {
        _purchaseState.value = IAPPurchaseState.Purchasing
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
    }

    // MARK: - SKProductsRequestDelegate

    override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
        val products = didReceiveResponse.products
        @Suppress("UNCHECKED_CAST")
        product = (products as List<SKProduct>).firstOrNull { it.productIdentifier == REMOVE_ADS_PRODUCT_ID }
        println("StoreKitHelper: Loaded ${products.size} products")
    }

    override fun request(request: SKRequest, didFailWithError: platform.Foundation.NSError) {
        println("StoreKitHelper: Failed to load products - ${didFailWithError.localizedDescription}")
    }

    // MARK: - SKPaymentTransactionObserver

    override fun paymentQueue(
        queue: SKPaymentQueue,
        updatedTransactions: List<*>
    ) {
        @Suppress("UNCHECKED_CAST")
        val transactions = updatedTransactions as List<SKPaymentTransaction>

        for (transaction in transactions) {
            when (transaction.transactionState) {
                SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                    handlePurchased(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                    handleFailed(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                    handleRestored(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> {
                    // Purchasing in progress
                }
                else -> {}
            }
        }
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        if (!_hasRemovedAds.value) {
            _purchaseState.value = IAPPurchaseState.Idle
        }
    }

    override fun paymentQueue(
        queue: SKPaymentQueue,
        restoreCompletedTransactionsFailedWithError: platform.Foundation.NSError
    ) {
        _purchaseState.value = IAPPurchaseState.Error(
            restoreCompletedTransactionsFailedWithError.localizedDescription
        )
    }

    // MARK: - Transaction Handling

    private fun handlePurchased(transaction: SKPaymentTransaction) {
        if (transaction.payment.productIdentifier == REMOVE_ADS_PRODUCT_ID) {
            _hasRemovedAds.value = true
            saveAdsRemovedState(true)
            _purchaseState.value = IAPPurchaseState.Purchased
        }
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }

    private fun handleFailed(transaction: SKPaymentTransaction) {
        val error = transaction.error
        if (error != null) {
            // Check if user cancelled
            if (error.code == 2L) { // SKErrorPaymentCancelled
                _purchaseState.value = IAPPurchaseState.Cancelled
            } else {
                _purchaseState.value = IAPPurchaseState.Error(error.localizedDescription)
            }
        } else {
            _purchaseState.value = IAPPurchaseState.Error("Unknown error")
        }
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }

    private fun handleRestored(transaction: SKPaymentTransaction) {
        if (transaction.originalTransaction?.payment?.productIdentifier == REMOVE_ADS_PRODUCT_ID) {
            _hasRemovedAds.value = true
            saveAdsRemovedState(true)
            _purchaseState.value = IAPPurchaseState.Purchased
        }
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }

    // MARK: - Persistence

    private fun loadAdsRemovedState(): Boolean {
        return NSUserDefaults.standardUserDefaults.boolForKey(PREFS_KEY_ADS_REMOVED)
    }

    private fun saveAdsRemovedState(removed: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(removed, PREFS_KEY_ADS_REMOVED)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}

/**
 * Purchase state for iOS StoreKit.
 */
sealed class IAPPurchaseState {
    data object Idle : IAPPurchaseState()
    data object Purchasing : IAPPurchaseState()
    data object Purchased : IAPPurchaseState()
    data object Cancelled : IAPPurchaseState()
    data class Error(val message: String) : IAPPurchaseState()
}
