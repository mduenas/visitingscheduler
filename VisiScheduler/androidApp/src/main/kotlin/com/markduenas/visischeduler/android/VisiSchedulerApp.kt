package com.markduenas.visischeduler.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.markduenas.visischeduler.BuildConfig
import com.markduenas.visischeduler.di.commonModule
import com.markduenas.visischeduler.di.dataModule
import com.markduenas.visischeduler.di.domainModule
import com.markduenas.visischeduler.di.platformModule
import com.markduenas.visischeduler.firebase.firebaseModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * VisiScheduler Android Application class.
 * Initializes Firebase services (Crashlytics, Analytics, Firestore) and Koin DI.
 */
class VisiSchedulerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize logging
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Initialize Firebase
        initializeFirebase()

        // Initialize Koin DI
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VisiSchedulerApp)
            modules(
                commonModule,
                platformModule(),
                dataModule,
                domainModule,
                firebaseModule,  // Firebase services and Firestore repositories
                androidAppModule
            )
        }

        Napier.d { "VisiScheduler Application initialized" }
    }

    /**
     * Initialize Firebase services.
     */
    private fun initializeFirebase() {
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(this)

            // Configure Crashlytics
            Firebase.crashlytics.apply {
                // Enable/disable based on build type and user consent
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

                // Set app version info
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)

                Napier.d { "Crashlytics initialized (collection: ${!BuildConfig.DEBUG})" }
            }

            // Configure Analytics
            Firebase.analytics.apply {
                // Enable/disable based on build type
                setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

                // Set default event parameters
                setDefaultEventParameters(android.os.Bundle().apply {
                    putString("app_version", BuildConfig.VERSION_NAME)
                    putString("platform", "android")
                })

                // Log app open event
                logEvent(FirebaseAnalytics.Event.APP_OPEN, null)

                Napier.d { "Analytics initialized (collection: ${!BuildConfig.DEBUG})" }
            }

            Napier.i { "Firebase services initialized successfully" }
        } catch (e: Exception) {
            Napier.e(e) { "Failed to initialize Firebase services" }
            // Don't crash the app if Firebase fails to initialize
        }
    }
}
