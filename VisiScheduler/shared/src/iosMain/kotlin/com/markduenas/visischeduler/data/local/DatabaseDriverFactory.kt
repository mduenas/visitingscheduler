package com.markduenas.visischeduler.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of DatabaseDriverFactory.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VisiSchedulerDatabase.Schema,
            name = "visischeduler.db"
        )
    }
}
