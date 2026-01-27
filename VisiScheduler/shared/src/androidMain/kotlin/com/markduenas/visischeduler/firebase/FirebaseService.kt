package com.markduenas.visischeduler.firebase

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics and Crashlytics service for VisiScheduler.
 * Handles event logging, user properties, and crash reporting.
 */
class FirebaseService {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private val crashlytics by lazy { Firebase.crashlytics }

    companion object {
        private const val TAG = "FirebaseService"

        // Custom event names
        const val EVENT_VISIT_SCHEDULED = "visit_scheduled"
        const val EVENT_VISIT_APPROVED = "visit_approved"
        const val EVENT_VISIT_DENIED = "visit_denied"
        const val EVENT_VISIT_CANCELLED = "visit_cancelled"
        const val EVENT_VISIT_CHECKED_IN = "visit_checked_in"
        const val EVENT_VISIT_CHECKED_OUT = "visit_checked_out"
        const val EVENT_VISITOR_ADDED = "visitor_added"
        const val EVENT_VISITOR_BLOCKED = "visitor_blocked"
        const val EVENT_MESSAGE_SENT = "message_sent"
        const val EVENT_RESTRICTION_CREATED = "restriction_created"

        // User properties
        const val PROP_USER_ROLE = "user_role"
        const val PROP_BENEFICIARY_COUNT = "beneficiary_count"
        const val PROP_VISITOR_COUNT = "visitor_count"
    }

    // ==================== Analytics ====================

    /**
     * Log a custom event to Firebase Analytics.
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        analytics.logEvent(eventName) {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Boolean -> param(key, if (value) 1L else 0L)
                    else -> param(key, value.toString())
                }
            }
        }
        Log.d(TAG, "Logged event: $eventName with params: $params")
    }

    /**
     * Log visit scheduled event.
     */
    fun logVisitScheduled(visitId: String, beneficiaryId: String, duration: Int) {
        logEvent(EVENT_VISIT_SCHEDULED, mapOf(
            "visit_id" to visitId,
            "beneficiary_id" to beneficiaryId,
            "duration_minutes" to duration
        ))
    }

    /**
     * Log visit approved event.
     */
    fun logVisitApproved(visitId: String, coordinatorId: String) {
        logEvent(EVENT_VISIT_APPROVED, mapOf(
            "visit_id" to visitId,
            "coordinator_id" to coordinatorId
        ))
    }

    /**
     * Log visit denied event.
     */
    fun logVisitDenied(visitId: String, reason: String?) {
        logEvent(EVENT_VISIT_DENIED, mapOf(
            "visit_id" to visitId,
            "reason" to (reason ?: "unspecified")
        ))
    }

    /**
     * Log check-in event.
     */
    fun logCheckIn(visitId: String, method: String) {
        logEvent(EVENT_VISIT_CHECKED_IN, mapOf(
            "visit_id" to visitId,
            "method" to method
        ))
    }

    /**
     * Log check-out event.
     */
    fun logCheckOut(visitId: String, durationMinutes: Int, rating: Int?) {
        logEvent(EVENT_VISIT_CHECKED_OUT, mapOf(
            "visit_id" to visitId,
            "duration_minutes" to durationMinutes,
            "rating" to (rating ?: 0)
        ))
    }

    /**
     * Log screen view event.
     */
    fun logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }

    /**
     * Set user ID for analytics tracking.
     */
    fun setUserId(userId: String?) {
        analytics.setUserId(userId)
        userId?.let { crashlytics.setUserId(it) }
    }

    /**
     * Set user property.
     */
    fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    /**
     * Set user role property.
     */
    fun setUserRole(role: String) {
        setUserProperty(PROP_USER_ROLE, role)
        crashlytics.setCustomKey("user_role", role)
    }

    // ==================== Crashlytics ====================

    /**
     * Log a non-fatal exception to Crashlytics.
     */
    fun logException(throwable: Throwable) {
        crashlytics.recordException(throwable)
        Log.e(TAG, "Logged exception to Crashlytics", throwable)
    }

    /**
     * Log a custom message to Crashlytics.
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Set a custom key-value pair for crash reports.
     */
    fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    /**
     * Enable or disable Crashlytics collection.
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    /**
     * Enable or disable Analytics collection.
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    // ==================== Scheduling Analytics ====================

    /**
     * Log time slot selection event.
     */
    fun logTimeSlotSelected(slotId: String, date: String, time: String) {
        logEvent("time_slot_selected", mapOf(
            "slot_id" to slotId,
            "date" to date,
            "time" to time
        ))
    }

    /**
     * Log slot availability check.
     */
    fun logSlotAvailabilityCheck(date: String, availableSlots: Int) {
        logEvent("slot_availability_check", mapOf(
            "date" to date,
            "available_slots" to availableSlots
        ))
    }

    /**
     * Log restriction applied event.
     */
    fun logRestrictionApplied(restrictionId: String, type: String) {
        logEvent(EVENT_RESTRICTION_CREATED, mapOf(
            "restriction_id" to restrictionId,
            "restriction_type" to type
        ))
    }

    /**
     * Log beneficiary profile view.
     */
    fun logBeneficiaryProfileView(beneficiaryId: String) {
        logEvent("beneficiary_profile_view", mapOf(
            "beneficiary_id" to beneficiaryId
        ))
    }

    /**
     * Log QR code generation.
     */
    fun logQrCodeGenerated(visitId: String) {
        logEvent("qr_code_generated", mapOf(
            "visit_id" to visitId
        ))
    }

    /**
     * Log QR code scan.
     */
    fun logQrCodeScanned(visitId: String, success: Boolean) {
        logEvent("qr_code_scanned", mapOf(
            "visit_id" to visitId,
            "success" to success
        ))
    }

    /**
     * Log notification interaction.
     */
    fun logNotificationInteraction(notificationId: String, action: String) {
        logEvent("notification_interaction", mapOf(
            "notification_id" to notificationId,
            "action" to action
        ))
    }

    /**
     * Log calendar view change.
     */
    fun logCalendarViewChange(viewType: String, date: String) {
        logEvent("calendar_view_change", mapOf(
            "view_type" to viewType,
            "date" to date
        ))
    }

    /**
     * Log search performed.
     */
    fun logSearch(searchQuery: String, resultsCount: Int) {
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, searchQuery)
            param("results_count", resultsCount.toLong())
        }
    }

    /**
     * Log sign up event.
     */
    fun logSignUp(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /**
     * Log login event.
     */
    fun logLogin(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /**
     * Log content share event.
     */
    fun logShare(contentType: String, itemId: String, method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            param(FirebaseAnalytics.Param.ITEM_ID, itemId)
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    // ==================== Error Tracking ====================

    /**
     * Log API error for tracking.
     */
    fun logApiError(endpoint: String, statusCode: Int, message: String?) {
        crashlytics.log("API Error: $endpoint - $statusCode: ${message ?: "No message"}")
        logEvent("api_error", mapOf(
            "endpoint" to endpoint,
            "status_code" to statusCode,
            "message" to (message ?: "unknown")
        ))
    }

    /**
     * Log network error.
     */
    fun logNetworkError(operation: String, message: String?) {
        crashlytics.log("Network Error during $operation: ${message ?: "Unknown"}")
        logEvent("network_error", mapOf(
            "operation" to operation,
            "message" to (message ?: "unknown")
        ))
    }

    /**
     * Log validation error.
     */
    fun logValidationError(field: String, error: String) {
        logEvent("validation_error", mapOf(
            "field" to field,
            "error" to error
        ))
    }

    // ==================== Performance Tracking ====================

    /**
     * Log app performance metrics.
     */
    fun logPerformanceMetric(name: String, durationMs: Long) {
        logEvent("performance_metric", mapOf(
            "metric_name" to name,
            "duration_ms" to durationMs
        ))
    }

    /**
     * Log screen load time.
     */
    fun logScreenLoadTime(screenName: String, loadTimeMs: Long) {
        logEvent("screen_load_time", mapOf(
            "screen_name" to screenName,
            "load_time_ms" to loadTimeMs
        ))
    }
}
