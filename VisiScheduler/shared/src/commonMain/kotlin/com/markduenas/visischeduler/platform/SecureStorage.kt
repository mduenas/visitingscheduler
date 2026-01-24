package com.markduenas.visischeduler.platform

/**
 * Interface for secure encrypted storage.
 */
interface SecureStorage {
    /**
     * Store a string value securely.
     */
    fun putString(key: String, value: String)

    /**
     * Retrieve a stored string value.
     */
    fun getString(key: String): String?

    /**
     * Store a boolean value securely.
     */
    fun putBoolean(key: String, value: Boolean)

    /**
     * Retrieve a stored boolean value.
     */
    fun getBoolean(key: String): Boolean?

    /**
     * Store a long value securely.
     */
    fun putLong(key: String, value: Long)

    /**
     * Retrieve a stored long value.
     */
    fun getLong(key: String): Long?

    /**
     * Remove a stored value.
     */
    fun remove(key: String)

    /**
     * Clear all stored values.
     */
    fun clear()

    /**
     * Check if a key exists.
     */
    fun contains(key: String): Boolean
}

/**
 * Expect declaration for platform-specific secure storage implementation.
 */
expect class SecureStorageImpl : SecureStorage
