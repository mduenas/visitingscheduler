package com.markduenas.visischeduler.domain.entities

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/**
 * Represents an available time slot for scheduling visits.
 */
@Serializable
data class TimeSlot(
    val id: String,
    val facilityId: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val maxCapacity: Int,
    val currentBookings: Int = 0,
    val isAvailable: Boolean = true,
    val slotType: SlotType = SlotType.REGULAR,
    val notes: String? = null
) {
    val remainingCapacity: Int
        get() = maxCapacity - currentBookings

    val isFull: Boolean
        get() = remainingCapacity <= 0

    val availabilityPercentage: Float
        get() = if (maxCapacity > 0) remainingCapacity.toFloat() / maxCapacity else 0f
}

/**
 * Type of time slot.
 */
@Serializable
enum class SlotType {
    /** Regular visiting hours */
    REGULAR,
    /** Extended hours for special circumstances */
    EXTENDED,
    /** Holiday visiting hours */
    HOLIDAY,
    /** Video call only slot */
    VIDEO_ONLY,
    /** Special event slot */
    SPECIAL_EVENT
}

/**
 * Represents the recurring schedule template for a facility.
 */
@Serializable
data class ScheduleTemplate(
    val id: String,
    val facilityId: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val slotDurationMinutes: Int = 60,
    val maxCapacityPerSlot: Int,
    val isActive: Boolean = true,
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Represents a blocked date/time when visits are not allowed.
 */
@Serializable
data class BlockedPeriod(
    val id: String,
    val facilityId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val reason: String,
    val blockType: BlockType,
    val createdBy: String,
    val createdAt: Instant
)

/**
 * Type of blocked period.
 */
@Serializable
enum class BlockType {
    /** Facility-wide closure */
    FACILITY_CLOSURE,
    /** Holiday closure */
    HOLIDAY,
    /** Maintenance period */
    MAINTENANCE,
    /** Emergency closure */
    EMERGENCY,
    /** Staff shortage */
    STAFF_SHORTAGE,
    /** Other reason */
    OTHER
}
