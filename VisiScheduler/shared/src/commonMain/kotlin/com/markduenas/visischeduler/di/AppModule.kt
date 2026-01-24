package com.markduenas.visischeduler.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection for the application.
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
            networkModule,
            databaseModule,
            repositoryModule,
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
