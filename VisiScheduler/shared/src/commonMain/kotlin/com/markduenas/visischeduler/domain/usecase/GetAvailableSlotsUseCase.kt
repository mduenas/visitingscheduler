package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for getting available time slots.
 */
class GetAvailableSlotsUseCase(
    private val timeSlotRepository: TimeSlotRepository,
    private val restrictionRepository: RestrictionRepository
) {
    /**
     * Get available time slots for a specific date and beneficiary.
     * @param beneficiaryId The beneficiary to visit
     * @param date The date to check
     * @param visitorId Optional visitor ID to check restrictions
     * @return Flow of available time slots
     */
    operator fun invoke(
        beneficiaryId: String,
        date: LocalDate,
        visitorId: String? = null
    ): Flow<List<TimeSlot>> {
        return timeSlotRepository.getAvailableSlotsForDate(beneficiaryId, date)
            .map { slots ->
                slots.filter { slot ->
                    slot.isAvailable && !slot.isFull
                }
            }
    }

    /**
     * Get available time slots for a date range.
     * @param beneficiaryId The beneficiary to visit
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return Flow of available time slots grouped by date
     */
    fun getForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, List<TimeSlot>>> {
        return timeSlotRepository.getAvailableSlotsForDateRange(beneficiaryId, startDate, endDate)
            .map { slots ->
                slots.filter { it.isAvailable && !it.isFull }
                    .groupBy { it.date }
            }
    }

    /**
     * Get available time slots for the next N days.
     * @param beneficiaryId The beneficiary to visit
     * @param days Number of days to look ahead (default 14)
     * @return Flow of available time slots grouped by date
     */
    fun getForNextDays(
        beneficiaryId: String,
        days: Int = 14
    ): Flow<Map<LocalDate, List<TimeSlot>>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val endDate = today.plus(DatePeriod(days = days))
        return getForDateRange(beneficiaryId, today, endDate)
    }

    /**
     * Check if a specific time slot is available.
     * @param slotId The time slot ID
     * @return Result indicating availability
     */
    suspend fun checkSlotAvailability(slotId: String): Result<SlotAvailability> {
        return timeSlotRepository.getSlotById(slotId).map { slot ->
            SlotAvailability(
                slot = slot,
                isAvailable = slot.isAvailable && !slot.isFull,
                remainingCapacity = slot.remainingCapacity,
                reason = when {
                    !slot.isAvailable -> "Slot is not available"
                    slot.isFull -> "Slot is fully booked"
                    else -> null
                }
            )
        }
    }

    /**
     * Get the next available slot for a beneficiary.
     * @param beneficiaryId The beneficiary to visit
     * @return Result containing the next available slot or null
     */
    suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?> {
        return timeSlotRepository.getNextAvailableSlot(beneficiaryId)
    }

    /**
     * Get slots with high availability (good for suggestions).
     * @param beneficiaryId The beneficiary to visit
     * @param date The date to check
     * @param minAvailabilityPercent Minimum availability percentage (0-1)
     * @return Flow of high-availability slots
     */
    fun getHighAvailabilitySlots(
        beneficiaryId: String,
        date: LocalDate,
        minAvailabilityPercent: Float = 0.5f
    ): Flow<List<TimeSlot>> {
        return timeSlotRepository.getAvailableSlotsForDate(beneficiaryId, date)
            .map { slots ->
                slots.filter { slot ->
                    slot.isAvailable &&
                    !slot.isFull &&
                    slot.availabilityPercentage >= minAvailabilityPercent
                }.sortedByDescending { it.availabilityPercentage }
            }
    }
}

/**
 * Represents the availability status of a time slot.
 */
data class SlotAvailability(
    val slot: TimeSlot,
    val isAvailable: Boolean,
    val remainingCapacity: Int,
    val reason: String? = null
)
