package com.markduenas.visischeduler.config

/**
 * AdMob configuration constants for VisiScheduler.
 * Contains ad unit IDs and configuration settings for both Android and iOS.
 */
object AdMobConstants {

    /**
     * AdMob Application ID (same for both platforms)
     */
    const val APPLICATION_ID = "ca-app-pub-7540731406850248~6456438881"

    /**
     * Android AdMob Banner Ad Unit ID
     */
    const val ANDROID_BANNER_AD_UNIT_ID = "ca-app-pub-7540731406850248/7111045003"

    /**
     * iOS AdMob Banner Ad Unit ID
     */
    const val IOS_BANNER_AD_UNIT_ID = "ca-app-pub-7540731406850248/5797963335"

    /**
     * Test banner ad unit IDs for development
     */
    const val ANDROID_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val IOS_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/2934735716"

    /**
     * Banner ad height in dp
     */
    const val BANNER_HEIGHT_DP = 50

    /**
     * Whether AdMob is enabled (can be controlled by feature flags)
     */
    const val ADMOB_ENABLED = true

    /**
     * Product ID for removing ads via in-app purchase
     */
    const val REMOVE_ADS_PRODUCT_ID = "com.markduenas.visischeduler.remove_ads"

    /**
     * Get the platform-specific banner ad unit ID
     */
    fun getBannerAdUnitId(isAndroid: Boolean, isDebug: Boolean = false): String {
        return when {
            isDebug && isAndroid -> ANDROID_TEST_BANNER_AD_UNIT_ID
            isDebug && !isAndroid -> IOS_TEST_BANNER_AD_UNIT_ID
            isAndroid -> ANDROID_BANNER_AD_UNIT_ID
            else -> IOS_BANNER_AD_UNIT_ID
        }
    }
}
