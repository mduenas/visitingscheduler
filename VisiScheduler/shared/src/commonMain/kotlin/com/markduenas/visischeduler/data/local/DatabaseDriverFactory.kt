package com.markduenas.visischeduler.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating platform-specific SQLDelight database drivers.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Helper to create the database instance.
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): VisiSchedulerDatabase {
    val driver = driverFactory.createDriver()
    return VisiSchedulerDatabase(driver)
}
