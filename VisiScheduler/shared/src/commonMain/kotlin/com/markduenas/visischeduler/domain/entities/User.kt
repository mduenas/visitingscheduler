package com.markduenas.visischeduler.domain.entities

import kotlinx.serialization.Contextual
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents the different roles a user can have in the VisiScheduler system.
 */
@Serializable
enum class Role {
    /** Full system access, can manage all users and settings */
    ADMIN,
    /** Primary coordinator - can approve/deny visits, manage schedules */
    PRIMARY_COORDINATOR,
    /** Secondary coordinator - limited approval authority */
    SECONDARY_COORDINATOR,
    /** Approved visitor - can schedule visits */
    APPROVED_VISITOR,
    /** Pending visitor - awaiting approval to use the system */
    PENDING_VISITOR
}

/**
 * Represents a user in the VisiScheduler system.
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: Role,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    @Contextual val lastLoginAt: Instant? = null,
    /** Associated beneficiary IDs for visitors */
    val associatedBeneficiaryIds: List<String> = emptyList(),
    /** Notification preferences */
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    /** Whether two-factor authentication is enabled */
    val mfaEnabled: Boolean = false,
    /** Email address used for MFA codes (may differ from login email) */
    val mfaEmail: String? = null
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun canApproveVisits(): Boolean =
        role == Role.ADMIN || role == Role.PRIMARY_COORDINATOR || role == Role.SECONDARY_COORDINATOR

    fun canScheduleVisits(): Boolean =
        role == Role.APPROVED_VISITOR || canApproveVisits()

    fun canManageUsers(): Boolean =
        role == Role.ADMIN

    fun canManageRestrictions(): Boolean =
        role == Role.ADMIN || role == Role.PRIMARY_COORDINATOR
}

/**
 * User notification preferences.
 */
@Serializable
data class NotificationPreferences(
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    val visitReminders: Boolean = true,
    val approvalNotifications: Boolean = true,
    val scheduleChanges: Boolean = true
)
