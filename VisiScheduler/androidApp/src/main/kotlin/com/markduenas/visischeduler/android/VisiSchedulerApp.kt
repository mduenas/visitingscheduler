package com.markduenas.visischeduler.android

import android.app.Application
import com.markduenas.visischeduler.BuildConfig
import com.markduenas.visischeduler.di.commonModule
import com.markduenas.visischeduler.di.dataModule
import com.markduenas.visischeduler.di.domainModule
import com.markduenas.visischeduler.di.platformModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * VisiScheduler Android Application class.
 */
class VisiSchedulerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize logging
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Initialize Koin DI
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@VisiSchedulerApp)
            modules(
                commonModule,
                platformModule(),
                dataModule,
                domainModule,
                androidAppModule
            )
        }

        Napier.d { "VisiScheduler Application initialized" }
    }
}
