package com.markduenas.visischeduler.di

import org.koin.dsl.module

/**
 * Database module providing SQLDelight database and DAOs.
 *
 * Note: The actual database driver is provided by platform-specific modules
 * (androidMain/iosMain) since SQLDelight requires different drivers per platform.
 */
val databaseModule = module {

    // Database instance is provided by platform modules
    // This module defines the common database-related dependencies

    // Example DAO providers would go here once the database is set up:
    // single { get<VisiSchedulerDatabase>().userQueries }
    // single { get<VisiSchedulerDatabase>().visitQueries }
    // single { get<VisiSchedulerDatabase>().restrictionQueries }
}
