package com.markduenas.visischeduler.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection for the application.
 *
 * Uses Firebase Firestore for cross-platform data storage via GitLive SDK.
 *
 * @param appDeclaration Optional Koin app configuration
 * @param platformModules Platform-specific modules (Android/iOS)
 */
fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
    platformModules: List<Module> = emptyList()
) = startKoin {
    appDeclaration()
    modules(
        listOf(
            databaseModule,      // Local SQLDelight database (for future offline caching)
            firestoreModule,     // Firebase Firestore repositories (cross-platform)
            useCaseModule,
            viewModelModule
        ) + platformModules
    )
}

/**
 * Simplified init for iOS which doesn't support KoinAppDeclaration lambda.
 */
fun initKoinIos(platformModules: List<Module> = emptyList()) = initKoin(
    appDeclaration = {},
    platformModules = platformModules
)
