package com.markduenas.visischeduler.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Helper function to initialize Koin with all modules.
 */
fun initKoin(additionalModules: List<Module> = emptyList()): KoinApplication {
    return startKoin {
        modules(
            listOf(
                commonModule,
                platformModule(),
                dataModule,
                domainModule
            ) + additionalModules
        )
    }
}

/**
 * Get all application modules.
 */
fun getAppModules(): List<Module> = listOf(
    commonModule,
    platformModule(),
    dataModule,
    domainModule
)
