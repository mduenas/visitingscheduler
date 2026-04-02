package com.markduenas.visischeduler.platform

/**
 * Cross-platform interface for biometric authentication (Face ID / Touch ID / Fingerprint).
 */
interface BiometricHandler {
    fun isAvailable(): Boolean
    fun canAuthenticate(): Boolean
    suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeButtonText: String
    ): BiometricResult
}

/**
 * Result of a biometric authentication attempt.
 */
sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val code: Int, val message: String) : BiometricResult()
    data object Cancelled : BiometricResult()
    data object NotAvailable : BiometricResult()
}
