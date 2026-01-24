package com.markduenas.visischeduler.domain.entities

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a notification in the VisiScheduler system.
 */
@Serializable
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val isRead: Boolean = false,
    val readAt: Instant? = null,
    val relatedEntityId: String? = null,
    val relatedEntityType: RelatedEntityType? = null,
    val actionUrl: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val expiresAt: Instant? = null,
    val createdAt: Instant
) {
    val isExpired: Boolean
        get() = expiresAt?.let { it < kotlinx.datetime.Clock.System.now() } ?: false
}

/**
 * Type of notification.
 */
@Serializable
enum class NotificationType {
    /** Visit request submitted */
    VISIT_REQUESTED,
    /** Visit approved */
    VISIT_APPROVED,
    /** Visit denied */
    VISIT_DENIED,
    /** Visit cancelled */
    VISIT_CANCELLED,
    /** Visit reminder (upcoming visit) */
    VISIT_REMINDER,
    /** Visit check-in confirmation */
    VISIT_CHECKED_IN,
    /** Visit completed */
    VISIT_COMPLETED,
    /** Schedule change notification */
    SCHEDULE_CHANGE,
    /** New restriction applied */
    RESTRICTION_APPLIED,
    /** User account status change */
    ACCOUNT_STATUS,
    /** System announcement */
    SYSTEM_ANNOUNCEMENT,
    /** Approval request (for coordinators) */
    APPROVAL_REQUEST,
    /** General information */
    INFO
}

/**
 * Priority level of a notification.
 */
@Serializable
enum class NotificationPriority {
    /** Low priority - can be batched */
    LOW,
    /** Normal priority */
    NORMAL,
    /** High priority - should be sent immediately */
    HIGH,
    /** Urgent - requires immediate attention */
    URGENT
}

/**
 * Type of related entity for deep linking.
 */
@Serializable
enum class RelatedEntityType {
    VISIT,
    BENEFICIARY,
    USER,
    RESTRICTION,
    FACILITY,
    TIME_SLOT
}

/**
 * Represents a notification channel configuration.
 */
@Serializable
data class NotificationChannel(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val supportedTypes: List<NotificationType>,
    val deliveryMethod: DeliveryMethod
)

/**
 * Method of notification delivery.
 */
@Serializable
enum class DeliveryMethod {
    /** Push notification */
    PUSH,
    /** Email */
    EMAIL,
    /** SMS text message */
    SMS,
    /** In-app notification */
    IN_APP
}
