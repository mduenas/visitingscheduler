package com.markduenas.visischeduler.data.repository

import android.app.Activity
import com.markduenas.visischeduler.data.billing.BillingManager
import com.markduenas.visischeduler.domain.repository.AdRepository
import com.markduenas.visischeduler.firebase.FirebaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Android implementation of AdRepository using Google Play Billing.
 */
class AdRepositoryImpl(
    private val billingManager: BillingManager,
    private val firebaseService: FirebaseService
) : AdRepository {

    private var currentActivity: Activity? = null

    /**
     * Set the current activity for launching purchase flows.
     */
    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    override fun shouldShowAds(): Flow<Boolean> {
        return billingManager.hasRemovedAds.map { hasRemoved -> !hasRemoved }
    }

    override suspend fun hasRemovedAds(): Boolean {
        return billingManager.hasRemovedAds.value
    }

    override suspend fun purchaseAdRemoval(): Result<Unit> = runCatching {
        val activity = currentActivity
            ?: throw Exception("No activity available for purchase flow")

        val success = billingManager.launchPurchaseFlow(activity)
        if (!success) {
            throw Exception("Failed to launch purchase flow")
        }
    }

    override suspend fun restorePurchase(): Result<Boolean> = runCatching {
        billingManager.restorePurchases()
    }

    override suspend fun getAdRemovalPrice(): Result<String> = runCatching {
        billingManager.getAdRemovalPrice()
            ?: throw Exception("Price not available")
    }

    override suspend fun recordAdImpression() {
        firebaseService.logEvent("ad_impression", mapOf("ad_type" to "banner"))
    }

    override suspend fun recordAdClick() {
        firebaseService.logEvent("ad_click", mapOf("ad_type" to "banner"))
    }
}
