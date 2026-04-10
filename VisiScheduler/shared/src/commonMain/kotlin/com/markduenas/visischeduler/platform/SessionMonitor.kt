package com.markduenas.visischeduler.platform

import com.markduenas.visischeduler.domain.repository.AuthRepository

/**
 * Handles app-foreground session lifecycle.
 * Call [onForeground] from each platform's app-active lifecycle callback.
 * Registered as a Koin singleton so both Android and iOS share the same instance.
 */
class SessionMonitor(private val authRepository: AuthRepository) {

    /**
     * Called when the app returns to the foreground.
     * Updates the current device's last-active timestamp and checks for remote revocation.
     */
    suspend fun onForeground() {
        // Update activity timestamp — if the user's session was revoked on another device,
        // getActiveSessions will show isRevoked=true. The UI layer can observe this.
        authRepository.updateCurrentSessionActivity()

        // Check if this device's session was revoked remotely
        authRepository.getActiveSessions().getOrNull()
            ?.firstOrNull { it.isCurrent && it.isRevoked }
            ?.let {
                // Session was revoked — sign out locally
                authRepository.logout()
            }
    }
}
