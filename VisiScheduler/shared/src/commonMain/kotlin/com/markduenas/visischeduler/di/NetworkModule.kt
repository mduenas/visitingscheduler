package com.markduenas.visischeduler.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Configuration for the HTTP client.
 */
data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutMs: Long = 15_000,
    val requestTimeoutMs: Long = 30_000,
    val socketTimeoutMs: Long = 30_000,
    val enableLogging: Boolean = true
)

/**
 * Interface for providing authentication tokens.
 * Platform-specific implementations should provide actual tokens.
 */
interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun refreshTokens(): BearerTokens?
}

/**
 * Network module providing HTTP client and related dependencies.
 */
val networkModule = module {

    // JSON serializer configuration
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
            coerceInputValues = true
            explicitNulls = false
        }
    }

    // HTTP Client
    single {
        val config = getOrNull<NetworkConfig>() ?: NetworkConfig(
            baseUrl = "https://api.visischeduler.com/v1"
        )
        val json = get<Json>()
        val tokenProvider = getOrNull<TokenProvider>()

        HttpClient {
            // JSON Content Negotiation
            install(ContentNegotiation) {
                json(json)
            }

            // Authentication
            if (tokenProvider != null) {
                install(Auth) {
                    bearer {
                        loadTokens {
                            val accessToken = tokenProvider.getAccessToken()
                            val refreshToken = tokenProvider.getRefreshToken()
                            if (accessToken != null && refreshToken != null) {
                                BearerTokens(accessToken, refreshToken)
                            } else {
                                null
                            }
                        }

                        refreshTokens {
                            tokenProvider.refreshTokens()
                        }

                        sendWithoutRequest { request ->
                            // Always send tokens to our API
                            request.url.host.contains("visischeduler.com")
                        }
                    }
                }
            }

            // Logging
            if (config.enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("HTTP: $message")
                        }
                    }
                    level = LogLevel.BODY
                }
            }

            // Timeouts
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.requestTimeoutMs
                socketTimeoutMillis = config.socketTimeoutMs
            }

            // Default Request Configuration
            defaultRequest {
                url(config.baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
