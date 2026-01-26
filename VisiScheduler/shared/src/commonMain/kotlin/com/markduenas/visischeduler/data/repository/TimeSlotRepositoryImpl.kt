package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Implementation of TimeSlotRepository.
 */
class TimeSlotRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase
) : TimeSlotRepository {

    override fun getAvailableSlotsForDate(beneficiaryId: String, date: LocalDate): Flow<List<TimeSlot>> = flow {
        // First emit cached data
        val cached = database.visiSchedulerQueries
            .selectAvailableSlots(date.toString())
            .executeAsList()
            .map { mapEntityToTimeSlot(it) }
        emit(cached)

        // Then fetch from API
        try {
            val slots = api.getAvailableSlots(
                beneficiaryId = beneficiaryId,
                startDate = date.toString(),
                endDate = date.toString()
            ).map { it.toDomain() }
            slots.forEach { cacheTimeSlot(it) }
            emit(slots)
        } catch (e: Exception) {
            // Keep cached data on error
        }
    }

    override fun getAvailableSlotsForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TimeSlot>> = flow {
        // First emit cached data
        val cached = database.visiSchedulerQueries
            .selectTimeSlotsByDateRange(startDate.toString(), endDate.toString())
            .executeAsList()
            .map { mapEntityToTimeSlot(it) }
        emit(cached)

        // Then fetch from API
        try {
            val slots = api.getAvailableSlots(
                beneficiaryId = beneficiaryId,
                startDate = startDate.toString(),
                endDate = endDate.toString()
            ).map { it.toDomain() }
            slots.forEach { cacheTimeSlot(it) }
            emit(slots)
        } catch (e: Exception) {
            // Keep cached data on error
        }
    }

    override suspend fun getSlotById(slotId: String): Result<TimeSlot> {
        return try {
            val slot = api.getSlotById(slotId).toDomain()
            cacheTimeSlot(slot)
            Result.success(slot)
        } catch (e: Exception) {
            // Try cache
            val cached = database.visiSchedulerQueries
                .selectTimeSlotById(slotId)
                .executeAsOneOrNull()
            if (cached != null) {
                Result.success(mapEntityToTimeSlot(cached))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?> {
        return try {
            // Get slots for next 30 days
            val today = kotlin.time.Clock.System.now()
                .toEpochMilliseconds()
                .let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
                .let { it.toLocalDateTime(TimeZone.currentSystemDefault()).date }

            val endDate = today.plus(30, DateTimeUnit.DAY)

            val slots = api.getAvailableSlots(
                beneficiaryId = beneficiaryId,
                startDate = today.toString(),
                endDate = endDate.toString()
            ).map { it.toDomain() }

            val nextSlot = slots
                .filter { it.isAvailable && !it.isFull }
                .minByOrNull { it.date.toEpochDays() * 10000 + it.startTime.toSecondOfDay() }

            Result.success(nextSlot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reserveSlot(slotId: String): Result<TimeSlot> {
        return try {
            // This would typically be an API call
            val slotResult = getSlotById(slotId)
            if (slotResult.isFailure) return slotResult

            val slot = slotResult.getOrNull()!!
            val updated = slot.copy(currentBookings = slot.currentBookings + 1)
            cacheTimeSlot(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun releaseSlot(slotId: String): Result<TimeSlot> {
        return try {
            val slotResult = getSlotById(slotId)
            if (slotResult.isFailure) return slotResult

            val slot = slotResult.getOrNull()!!
            val updated = slot.copy(currentBookings = maxOf(0, slot.currentBookings - 1))
            cacheTimeSlot(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncSlots(): Result<Unit> {
        return try {
            // Get current date range for syncing
            val today = kotlin.time.Clock.System.now()
                .toEpochMilliseconds()
                .let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
                .let { it.toLocalDateTime(TimeZone.currentSystemDefault()).date }

            val endDate = today.plus(30, DateTimeUnit.DAY)

            // This would need beneficiary ID in real implementation
            // For now, just clear old slots
            database.visiSchedulerQueries.deleteAllTimeSlots()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cacheTimeSlot(slot: TimeSlot) {
        database.visiSchedulerQueries.insertTimeSlot(
            id = slot.id,
            facilityId = slot.facilityId,
            date = slot.date.toString(),
            startTime = slot.startTime.toString(),
            endTime = slot.endTime.toString(),
            maxCapacity = slot.maxCapacity.toLong(),
            currentBookings = slot.currentBookings.toLong(),
            isAvailable = if (slot.isAvailable) 1L else 0L,
            slotType = slot.slotType.name,
            notes = slot.notes
        )
    }

    private fun mapEntityToTimeSlot(entity: com.markduenas.visischeduler.data.local.TimeSlotEntity): TimeSlot {
        return TimeSlot(
            id = entity.id,
            facilityId = entity.facilityId,
            date = LocalDate.parse(entity.date),
            startTime = LocalTime.parse(entity.startTime),
            endTime = LocalTime.parse(entity.endTime),
            maxCapacity = entity.maxCapacity.toInt(),
            currentBookings = entity.currentBookings.toInt(),
            isAvailable = entity.isAvailable == 1L,
            slotType = SlotType.valueOf(entity.slotType),
            notes = entity.notes
        )
    }
}
