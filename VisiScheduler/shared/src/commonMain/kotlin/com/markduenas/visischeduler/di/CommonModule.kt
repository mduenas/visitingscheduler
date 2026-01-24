package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.local.createDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common module providing shared dependencies.
 */
val commonModule = module {
    // JSON serialization
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = true
            coerceInputValues = true
        }
    }

    // HTTP Client
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    // API
    single {
        VisiSchedulerApi(
            client = get(),
            baseUrl = "https://api.visischeduler.com/v1"
        )
    }

    // Database
    single { createDatabase(get()) }
}

/**
 * Platform-specific module - must be provided by each platform.
 */
expect fun platformModule(): Module
