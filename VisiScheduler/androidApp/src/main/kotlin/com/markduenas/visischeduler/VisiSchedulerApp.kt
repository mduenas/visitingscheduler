package com.markduenas.visischeduler

import android.app.Application
import com.markduenas.visischeduler.di.initKoin
import com.markduenas.visischeduler.platform.androidPlatformModule
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
 */
class VisiSchedulerApp : Application() {

    override fun onCreate() {
        super.onCreate()

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
            platformModules = listOf(androidPlatformModule)
        )

        Napier.d { "VisiScheduler application initialized" }
    }
}
