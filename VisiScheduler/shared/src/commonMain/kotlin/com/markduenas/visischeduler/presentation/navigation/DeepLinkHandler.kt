package com.markduenas.visischeduler.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deep link types supported by VisiScheduler.
 * Format: visischeduler://<type>/<params>
 */
sealed class DeepLink {
    /**
     * Visit detail deep link.
     * Format: visischeduler://visit/{visitId}
     */
    data class Visit(val visitId: String) : DeepLink()

    /**
     * Invitation acceptance deep link.
     * Format: visischeduler://invite/{code}
     */
    data class Invitation(val inviteCode: String) : DeepLink()

    /**
     * Calendar deep link with optional date.
     * Format: visischeduler://calendar/{date?}
     * Date format: YYYY-MM-DD
     */
    data class Calendar(val date: String? = null) : DeepLink()

    /**
     * Message/conversation deep link.
     * Format: visischeduler://message/{conversationId}
     */
    data class Message(val conversationId: String) : DeepLink()

    /**
     * Notification deep link.
     * Format: visischeduler://notifications
     */
    data object Notification : DeepLink()

    /**
     * Profile deep link.
     * Format: visischeduler://profile
     */
    data object Profile : DeepLink()

    /**
     * Settings deep link.
     * Format: visischeduler://settings
     */
    data object Settings : DeepLink()

    /**
     * Unknown/unsupported deep link.
     */
    data class Unknown(val rawUrl: String) : DeepLink()

    companion object {
        /** URL scheme for the app */
        const val SCHEME = "visischeduler"

        /** Alternative HTTPS scheme for universal links */
        const val HTTPS_HOST = "app.visischeduler.com"
    }
}

/**
 * Handler for processing and managing deep links.
 */
class DeepLinkHandler {
    private val _pendingDeepLink = MutableStateFlow<DeepLink?>(null)
    val pendingDeepLink: DeepLink? get() = _pendingDeepLink.value

    private val _deepLinkFlow = MutableStateFlow<DeepLink?>(null)
    val deepLinkFlow: StateFlow<DeepLink?> = _deepLinkFlow.asStateFlow()

    /**
     * Parse a URL string into a DeepLink.
     *
     * Supports:
     * - visischeduler://visit/{visitId}
     * - visischeduler://invite/{code}
     * - visischeduler://calendar/{date?}
     * - visischeduler://message/{conversationId}
     * - visischeduler://notifications
     * - visischeduler://profile
     * - visischeduler://settings
     * - https://app.visischeduler.com/visit/{visitId}
     *
     * @param url The URL to parse
     * @return Parsed DeepLink or Unknown if not recognized
     */
    fun parseDeepLink(url: String): DeepLink {
        return try {
            val normalizedUrl = normalizeUrl(url)
            val (path, params) = extractPathAndParams(normalizedUrl)

            when {
                path.startsWith("visit/") -> {
                    val visitId = path.removePrefix("visit/")
                    if (visitId.isNotBlank()) DeepLink.Visit(visitId)
                    else DeepLink.Unknown(url)
                }
                path.startsWith("invite/") -> {
                    val inviteCode = path.removePrefix("invite/")
                    if (inviteCode.isNotBlank()) DeepLink.Invitation(inviteCode)
                    else DeepLink.Unknown(url)
                }
                path.startsWith("calendar") -> {
                    val date = if (path.contains("/")) {
                        path.removePrefix("calendar/").takeIf { it.isNotBlank() }
                    } else {
                        params["date"]
                    }
                    DeepLink.Calendar(date)
                }
                path.startsWith("message/") -> {
                    val conversationId = path.removePrefix("message/")
                    if (conversationId.isNotBlank()) DeepLink.Message(conversationId)
                    else DeepLink.Unknown(url)
                }
                path == "notifications" || path.startsWith("notifications") -> {
                    DeepLink.Notification
                }
                path == "profile" || path.startsWith("profile") -> {
                    DeepLink.Profile
                }
                path == "settings" || path.startsWith("settings") -> {
                    DeepLink.Settings
                }
                else -> DeepLink.Unknown(url)
            }
        } catch (e: Exception) {
            DeepLink.Unknown(url)
        }
    }

    /**
     * Handle an incoming deep link URL.
     *
     * @param url The deep link URL
     * @param deferIfNotReady If true, stores the deep link for later processing
     */
    fun handleDeepLink(url: String, deferIfNotReady: Boolean = true) {
        val deepLink = parseDeepLink(url)
        if (deferIfNotReady) {
            _pendingDeepLink.value = deepLink
        }
        _deepLinkFlow.value = deepLink
    }

    /**
     * Clear the pending deep link after it has been processed.
     */
    fun clearPendingDeepLink() {
        _pendingDeepLink.value = null
    }

    /**
     * Clear the deep link flow.
     */
    fun clearDeepLink() {
        _deepLinkFlow.value = null
        _pendingDeepLink.value = null
    }

    /**
     * Check if there's a pending deep link.
     */
    fun hasPendingDeepLink(): Boolean = _pendingDeepLink.value != null

    /**
     * Normalize URL to extract path consistently.
     */
    private fun normalizeUrl(url: String): String {
        return when {
            // Custom scheme: visischeduler://path
            url.startsWith("${DeepLink.SCHEME}://") -> {
                url.removePrefix("${DeepLink.SCHEME}://")
            }
            // Universal link: https://app.visischeduler.com/path
            url.contains(DeepLink.HTTPS_HOST) -> {
                url.substringAfter("${DeepLink.HTTPS_HOST}/")
            }
            // HTTP variant
            url.contains("visischeduler.com") -> {
                url.substringAfter("visischeduler.com/")
            }
            else -> url
        }
    }

    /**
     * Extract path and query parameters from normalized URL.
     */
    private fun extractPathAndParams(normalizedUrl: String): Pair<String, Map<String, String>> {
        val parts = normalizedUrl.split("?", limit = 2)
        val path = parts[0].trimEnd('/')
        val params = if (parts.size > 1) {
            parts[1].split("&")
                .mapNotNull { param ->
                    val keyValue = param.split("=", limit = 2)
                    if (keyValue.size == 2) {
                        keyValue[0] to keyValue[1]
                    } else null
                }
                .toMap()
        } else {
            emptyMap()
        }
        return path to params
    }
}

/**
 * Build a deep link URL.
 */
object DeepLinkBuilder {
    /**
     * Build a visit deep link.
     */
    fun visit(visitId: String): String =
        "${DeepLink.SCHEME}://visit/$visitId"

    /**
     * Build an invitation deep link.
     */
    fun invitation(inviteCode: String): String =
        "${DeepLink.SCHEME}://invite/$inviteCode"

    /**
     * Build a calendar deep link.
     */
    fun calendar(date: String? = null): String =
        if (date != null) "${DeepLink.SCHEME}://calendar/$date"
        else "${DeepLink.SCHEME}://calendar"

    /**
     * Build a message deep link.
     */
    fun message(conversationId: String): String =
        "${DeepLink.SCHEME}://message/$conversationId"

    /**
     * Build a notifications deep link.
     */
    fun notifications(): String =
        "${DeepLink.SCHEME}://notifications"

    /**
     * Build a profile deep link.
     */
    fun profile(): String =
        "${DeepLink.SCHEME}://profile"

    /**
     * Build a settings deep link.
     */
    fun settings(): String =
        "${DeepLink.SCHEME}://settings"

    /**
     * Build a universal link (HTTPS) version.
     */
    fun universal(path: String): String =
        "https://${DeepLink.HTTPS_HOST}/$path"
}
