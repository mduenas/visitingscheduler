package com.markduenas.visischeduler.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of DatabaseDriverFactory.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VisiSchedulerDatabase.Schema,
            context = context,
            name = "visischeduler.db"
        )
    }
}
