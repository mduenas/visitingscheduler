package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for time slot operations.
 */
interface TimeSlotRepository {
    /**
     * Get available slots for a specific date.
     */
    fun getAvailableSlotsForDate(beneficiaryId: String, date: LocalDate): Flow<List<TimeSlot>>

    /**
     * Get available slots for a date range.
     */
    fun getAvailableSlotsForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TimeSlot>>

    /**
     * Get a specific slot by ID.
     */
    suspend fun getSlotById(slotId: String): Result<TimeSlot>

    /**
     * Get the next available slot.
     */
    suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?>

    /**
     * Reserve a slot (decrement availability).
     */
    suspend fun reserveSlot(slotId: String): Result<TimeSlot>

    /**
     * Release a slot (increment availability).
     */
    suspend fun releaseSlot(slotId: String): Result<TimeSlot>

    /**
     * Sync slots from remote server.
     */
    suspend fun syncSlots(): Result<Unit>
}
