package com.markduenas.visischeduler.config

/**
 * Global application configuration.
 */
object AppConfig {
    /**
     * The environment the app is running in.
     */
    val environment: AppEnvironment = AppEnvironment.DEVELOPMENT

    /**
     * API Base URL based on environment.
     */
    val apiBaseUrl: String = when (environment) {
        AppEnvironment.DEVELOPMENT -> "https://dev-api.visischeduler.com/v1"
        AppEnvironment.STAGING -> "https://staging-api.visischeduler.com/v1"
        AppEnvironment.PRODUCTION -> "https://api.visischeduler.com/v1"
    }

    /**
     * Firebase project configuration (placeholders).
     */
    val firebaseProjectId: String = when (environment) {
        AppEnvironment.DEVELOPMENT -> "visischeduler-dev"
        else -> "visischeduler-prod"
    }

    /**
     * Feature flags.
     */
    object Features {
        const val ENABLE_OFFLINE_SYNC = true
        const val ENABLE_ANALYTICS = true
        const val ENABLE_CRASHLYTICS = true
    }
}

/**
 * Supported application environments.
 */
enum class AppEnvironment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}
