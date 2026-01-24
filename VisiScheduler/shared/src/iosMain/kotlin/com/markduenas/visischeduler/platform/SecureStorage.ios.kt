package com.markduenas.visischeduler.platform

import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

/**
 * iOS implementation of SecureStorage using Keychain.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SecureStorageImpl : SecureStorage {

    private val serviceName = "com.markduenas.visischeduler"
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun putString(key: String, value: String) {
        val data = value.toNSData() ?: return
        saveToKeychain(key, data)
    }

    override fun getString(key: String): String? {
        val data = loadFromKeychain(key) ?: return null
        return data.toKotlinString()
    }

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, key)
    }

    override fun getBoolean(key: String): Boolean? {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.boolForKey(key)
        } else {
            null
        }
    }

    override fun putLong(key: String, value: Long) {
        userDefaults.setInteger(value, key)
    }

    override fun getLong(key: String): Long? {
        return if (userDefaults.objectForKey(key) != null) {
            userDefaults.integerForKey(key)
        } else {
            null
        }
    }

    override fun remove(key: String) {
        deleteFromKeychain(key)
        userDefaults.removeObjectForKey(key)
    }

    override fun clear() {
        // Note: This is a simplified implementation
        // In production, you'd want to track all keys
    }

    override fun contains(key: String): Boolean {
        return loadFromKeychain(key) != null || userDefaults.objectForKey(key) != null
    }

    private fun saveToKeychain(key: String, data: NSData) {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecValueData to data
        )

        // Try to delete existing item first
        deleteFromKeychain(key)

        // Add new item
        SecItemAdd(query as CFDictionaryRef, null)
    }

    private fun loadFromKeychain(key: String): NSData? {
        return memScoped {
            val query = mapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to serviceName,
                kSecAttrAccount to key,
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne
            )

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

            if (status == 0) {
                result.value as? NSData
            } else {
                null
            }
        }
    }

    private fun deleteFromKeychain(key: String) {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key
        )
        SecItemDelete(query as CFDictionaryRef)
    }

    private fun String.toNSData(): NSData? {
        return (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)
    }

    private fun NSData.toKotlinString(): String? {
        return NSString.create(data = this, encoding = NSUTF8StringEncoding) as? String
    }
}
