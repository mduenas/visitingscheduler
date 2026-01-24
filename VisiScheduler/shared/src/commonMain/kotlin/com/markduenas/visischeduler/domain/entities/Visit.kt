package com.markduenas.visischeduler.domain.entities

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Represents the status of a visit.
 */
@Serializable
enum class VisitStatus {
    /** Visit is awaiting approval */
    PENDING,
    /** Visit has been approved */
    APPROVED,
    /** Visit request was denied */
    DENIED,
    /** Visit was completed successfully */
    COMPLETED,
    /** Visit was cancelled by visitor or coordinator */
    CANCELLED,
    /** Visitor did not show up */
    NO_SHOW
}

/**
 * Represents a scheduled visit in the VisiScheduler system.
 */
@Serializable
data class Visit(
    val id: String,
    val beneficiaryId: String,
    val visitorId: String,
    val scheduledDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: VisitStatus = VisitStatus.PENDING,
    val visitType: VisitType = VisitType.IN_PERSON,
    val purpose: String? = null,
    val notes: String? = null,
    val additionalVisitors: List<AdditionalVisitor> = emptyList(),
    val checkInTime: Instant? = null,
    val checkOutTime: Instant? = null,
    val approvedBy: String? = null,
    val approvedAt: Instant? = null,
    val denialReason: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isUpcoming: Boolean
        get() = status == VisitStatus.APPROVED || status == VisitStatus.PENDING

    val isPast: Boolean
        get() = status == VisitStatus.COMPLETED || status == VisitStatus.NO_SHOW

    val isCancellable: Boolean
        get() = status == VisitStatus.PENDING || status == VisitStatus.APPROVED

    val totalVisitorCount: Int
        get() = 1 + additionalVisitors.size
}

/**
 * Type of visit.
 */
@Serializable
enum class VisitType {
    /** Standard in-person visit */
    IN_PERSON,
    /** Video call visit */
    VIDEO_CALL,
    /** Window visit (limited contact) */
    WINDOW_VISIT,
    /** Special event visit */
    SPECIAL_EVENT
}

/**
 * Represents an additional visitor accompanying the primary visitor.
 */
@Serializable
data class AdditionalVisitor(
    val id: String,
    val firstName: String,
    val lastName: String,
    val relationship: String,
    val isMinor: Boolean = false,
    val age: Int? = null
) {
    val fullName: String
        get() = "$firstName $lastName"
}
