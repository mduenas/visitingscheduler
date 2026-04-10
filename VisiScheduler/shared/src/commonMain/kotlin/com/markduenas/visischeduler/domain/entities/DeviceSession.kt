package com.markduenas.visischeduler.domain.entities

import kotlin.time.Instant

/**
 * Represents an active or revoked login session on a specific device.
 */
data class DeviceSession(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val userId: String,
    val createdAt: Instant,
    val lastActiveAt: Instant,
    val isRevoked: Boolean = false,
    /** True if this session belongs to the current device. Computed client-side, not stored. */
    val isCurrent: Boolean = false
)
