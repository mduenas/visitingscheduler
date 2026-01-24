package com.markduenas.visischeduler.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing ad state and in-app purchases.
 */
interface AdRepository {

    /**
     * Observe whether ads should be shown.
     * Returns false if user has purchased ad removal.
     */
    fun shouldShowAds(): Flow<Boolean>

    /**
     * Check if user has purchased ad removal.
     */
    suspend fun hasRemovedAds(): Boolean

    /**
     * Purchase ad removal.
     * @return Result indicating success or failure of the purchase
     */
    suspend fun purchaseAdRemoval(): Result<Unit>

    /**
     * Restore previous ad removal purchase.
     * @return Result indicating whether a previous purchase was found
     */
    suspend fun restorePurchase(): Result<Boolean>

    /**
     * Get the price for ad removal as a formatted string.
     */
    suspend fun getAdRemovalPrice(): Result<String>

    /**
     * Record ad impression for analytics.
     */
    suspend fun recordAdImpression()

    /**
     * Record ad click for analytics.
     */
    suspend fun recordAdClick()
}
