package com.markduenas.visischeduler.platform

/**
 * Platform information interface.
 */
interface Platform {
    val name: String
    val version: String
    val isDebug: Boolean
}

/**
 * Expect declaration for platform-specific implementation.
 */
expect fun getPlatform(): Platform
