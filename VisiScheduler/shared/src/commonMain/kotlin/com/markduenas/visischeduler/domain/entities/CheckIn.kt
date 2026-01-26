package com.markduenas.visischeduler.domain.entities

import kotlinx.serialization.Contextual
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a check-in/check-out record for a visit.
 */
@Serializable
data class CheckIn(
    val id: String,
    val visitId: String,
    @Contextual val checkInTime: Instant,
    @Contextual val checkOutTime: Instant?,
    val method: CheckInMethod,
    val notes: String?,
    val rating: Int? // 1-5 post-visit rating
) {
    /**
     * Returns true if the visitor has checked out.
     */
    val isCheckedOut: Boolean
        get() = checkOutTime != null

    /**
     * Returns true if the check-in is currently active (checked in but not checked out).
     */
    val isActive: Boolean
        get() = checkOutTime == null

    /**
     * Returns the duration of the visit in milliseconds if checked out.
     */
    val durationMillis: Long?
        get() = checkOutTime?.let { it.toEpochMilliseconds() - checkInTime.toEpochMilliseconds() }

    /**
     * Validates that the rating is within the valid range (1-5).
     */
    fun isValidRating(): Boolean = rating == null || rating in 1..5
}

/**
 * Method used to check in for a visit.
 */
@Serializable
enum class CheckInMethod {
    /** Check-in using QR code scanning */
    QR_CODE,
    /** Manual check-in by staff or visitor */
    MANUAL,
    /** Automatic check-in based on location or time */
    AUTOMATIC
}

/**
 * Data embedded in a QR code for visit verification.
 */
@Serializable
data class QrCodeData(
    val visitId: String,
    val visitorId: String,
    @Contextual val validFrom: Instant,
    @Contextual val validUntil: Instant,
    val signature: String // For verification
) {
    /**
     * Checks if the QR code is currently valid based on time.
     */
    fun isValid(currentTime: Instant): Boolean {
        return currentTime >= validFrom && currentTime <= validUntil
    }

    /**
     * Returns true if the QR code has expired.
     */
    fun isExpired(currentTime: Instant): Boolean {
        return currentTime > validUntil
    }

    /**
     * Returns true if the QR code is not yet valid.
     */
    fun isNotYetValid(currentTime: Instant): Boolean {
        return currentTime < validFrom
    }
}

/**
 * Status of QR code validation.
 */
@Serializable
sealed class QrValidationResult {
    data class Valid(val visit: Visit) : QrValidationResult()
    data class Expired(@Contextual val expiredAt: Instant) : QrValidationResult()
    data class NotYetValid(@Contextual val validFrom: Instant) : QrValidationResult()
    data class InvalidSignature(val message: String) : QrValidationResult()
    data class VisitNotFound(val visitId: String) : QrValidationResult()
    data class AlreadyCheckedIn(val checkIn: CheckIn) : QrValidationResult()
    data class VisitCancelled(val visit: Visit) : QrValidationResult()
}

/**
 * Represents an expected visitor for today's schedule.
 */
@Serializable
data class ExpectedVisitor(
    val visit: Visit,
    val visitorName: String,
    val visitorPhotoUrl: String?,
    val beneficiaryName: String,
    val beneficiaryRoom: String?,
    val checkInStatus: ExpectedVisitorStatus
)

/**
 * Status of an expected visitor.
 */
@Serializable
enum class ExpectedVisitorStatus {
    /** Visitor has not yet arrived */
    NOT_ARRIVED,
    /** Visitor has checked in */
    CHECKED_IN,
    /** Visitor has checked out */
    CHECKED_OUT,
    /** Visitor is late (past scheduled time) */
    LATE,
    /** Visitor was marked as no-show */
    NO_SHOW
}

/**
 * Data for a digital visitor badge.
 */
@Serializable
data class VisitorBadge(
    val visit: Visit,
    val visitorName: String,
    val visitorPhotoUrl: String?,
    val beneficiaryName: String,
    val beneficiaryRoom: String?,
    @Contextual val checkInTime: Instant,
    @Contextual val validUntil: Instant,
    val qrCodeData: QrCodeData,
    val badgeNumber: String
) {
    /**
     * Returns true if the badge is still valid.
     */
    fun isValid(currentTime: Instant): Boolean {
        return currentTime <= validUntil
    }

    /**
     * Returns the remaining time in milliseconds.
     */
    fun remainingTimeMillis(currentTime: Instant): Long {
        return (validUntil.toEpochMilliseconds() - currentTime.toEpochMilliseconds()).coerceAtLeast(0)
    }
}
