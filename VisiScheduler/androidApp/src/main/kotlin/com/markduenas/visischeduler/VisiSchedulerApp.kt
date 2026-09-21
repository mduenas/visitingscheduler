package com.markduenas.visischeduler

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.markduenas.visischeduler.data.billing.BillingManager
import com.markduenas.visischeduler.di.adModule
import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.di.firestoreModule
import com.markduenas.visischeduler.platform.androidPlatformModule
import org.koin.android.ext.android.inject
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Main application class for VisiScheduler Android app.
 *
 * Responsible for:
 * - Initializing Koin dependency injection
 * - Setting up logging
 * - Configuring global application state
 * - Initializing Firebase
 * - Initializing AdMob
 */
class VisiSchedulerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize AdMob
        // Register test devices so dev/QA taps never count as real ad traffic
        // (Google logs your device's real hash to Logcat the first time an ad
        // loads on it: "Use RequestConfiguration.Builder().setTestDeviceIds(...)"
        // - copy that hash into the list below.)
        MobileAds.setRequestConfiguration(
            com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR
                        // "ADD_YOUR_REAL_DEVICE_HASH_HERE",
                    )
                )
                .build()
        )
        MobileAds.initialize(this) { }

        // Initialize Napier logging
        if (BuildConfig.ENABLE_LOGGING) {
            Napier.base(DebugAntilog())
        }

        // Initialize Koin
        initKoin(
            appDeclaration = {
                androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
                androidContext(this@VisiSchedulerApp)
            },
            platformModules = listOf(androidPlatformModule, firestoreModule, adModule)
        )

        // Initialize Billing Manager
        val billingManager: BillingManager by inject()
        billingManager.initialize()

        Napier.d { "VisiScheduler application initialized with Firebase, AdMob, and Billing" }
    }
}
