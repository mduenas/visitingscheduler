package com.markduenas.visischeduler.platform


/**
 * Types of permissions required by the app.
 */
enum class Permission {
    CAMERA,
    NOTIFICATIONS,
    LOCATION
}

/**
 * Status of a specific permission.
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_DETERMINED,
    SHOW_RATIONALE
}

/**
 * Platform-agnostic interface for managing system permissions.
 */
interface PermissionManager {
    /**
     * Check the current status of a permission.
     */
    suspend fun checkPermission(permission: Permission): PermissionStatus

    /**
     * Request a specific permission.
     */
    suspend fun requestPermission(permission: Permission): PermissionStatus

    /**
     * Opens the app settings screen so user can manually grant permissions.
     */
    fun openAppSettings()
}

/**
 * Factory function to get the platform-specific PermissionManager.
 */
expect fun getPermissionManager(): PermissionManager
