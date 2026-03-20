package com.markduenas.visischeduler.platform

/**
 * Platform-agnostic URL opener interface.
 */
interface UrlOpener {
    fun open(url: String)
}

/**
 * Expect declaration for platform-specific URL opener.
 */
expect class UrlOpenerImpl : UrlOpener
