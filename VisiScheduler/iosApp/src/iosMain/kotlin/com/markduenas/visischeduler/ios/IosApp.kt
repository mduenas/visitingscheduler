package com.markduenas.visischeduler.ios

import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.platform.getPlatform
import platform.Foundation.NSLog

/**
 * iOS Application entry point helpers.
 */
object IosApp {
    /**
     * Initialize the iOS application.
     * Call this from AppDelegate.
     */
    fun initialize() {
        // Initialize Koin dependency injection
        initKoin()

        // Log platform info
        val platform = getPlatform()
        NSLog("VisiScheduler initialized on ${platform.name}")
    }

    /**
     * Check if the app is running in debug mode.
     */
    fun isDebugMode(): Boolean {
        return getPlatform().isDebug
    }
}
