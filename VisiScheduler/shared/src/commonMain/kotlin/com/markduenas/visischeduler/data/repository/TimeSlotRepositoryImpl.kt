package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate

/**
 * Implementation of TimeSlotRepository.
 */
class TimeSlotRepositoryImpl(
    private val database: VisiSchedulerDatabase
) : TimeSlotRepository {

    override fun getAvailableSlotsForDate(beneficiaryId: String, date: LocalDate): Flow<List<TimeSlot>> = flowOf(emptyList())

    override fun getAvailableSlotsForDateRange(beneficiaryId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<TimeSlot>> = flowOf(emptyList())

    override suspend fun getSlotById(slotId: String): Result<TimeSlot> = Result.failure(Exception("Not implemented"))

    override suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?> = Result.success(null)

    override suspend fun reserveSlot(slotId: String): Result<TimeSlot> = Result.failure(Exception("Not implemented"))

    override suspend fun releaseSlot(slotId: String): Result<TimeSlot> = Result.failure(Exception("Not implemented"))

    override suspend fun syncSlots(): Result<Unit> = Result.success(Unit)
}
