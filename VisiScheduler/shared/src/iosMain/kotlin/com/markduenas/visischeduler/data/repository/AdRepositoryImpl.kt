package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.billing.IAPPurchaseState
import com.markduenas.visischeduler.data.billing.StoreKitHelper
import com.markduenas.visischeduler.domain.repository.AdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSLog

/**
 * iOS implementation of AdRepository using StoreKit.
 */
class AdRepositoryImpl(
    private val storeKitHelper: StoreKitHelper
) : AdRepository {

    override fun shouldShowAds(): Flow<Boolean> {
        return storeKitHelper.hasRemovedAds.map { hasRemoved -> !hasRemoved }
    }

    override suspend fun hasRemovedAds(): Boolean {
        return storeKitHelper.hasRemovedAds.value
    }

    override suspend fun purchaseAdRemoval(): Result<Unit> = runCatching {
        val success = storeKitHelper.purchaseAdRemoval()
        if (!success) {
            throw Exception("Failed to initiate purchase")
        }
        // Note: The actual purchase result is delivered via StateFlow
        // The UI should observe purchaseState for the result
    }

    override suspend fun restorePurchase(): Result<Boolean> = runCatching {
        storeKitHelper.restorePurchases()
        // Return current state - will be updated asynchronously
        storeKitHelper.hasRemovedAds.value
    }

    override suspend fun getAdRemovalPrice(): Result<String> = runCatching {
        storeKitHelper.getFormattedPrice()
            ?: throw Exception("Price not available")
    }

    override suspend fun recordAdImpression() {
        // Log to Firebase Analytics on iOS
        NSLog("Ad impression recorded")
    }

    override suspend fun recordAdClick() {
        // Log to Firebase Analytics on iOS
        NSLog("Ad click recorded")
    }
}
