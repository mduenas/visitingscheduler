package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of TimeSlotRepository.
 */
class CommonFirestoreTimeSlotRepository(
    private val firestore: FirestoreDatabase
) : TimeSlotRepository {

    override fun getAvailableSlotsForDate(
        beneficiaryId: String,
        date: LocalDate
    ): Flow<List<TimeSlot>> {
        return firestore.listenToAvailableTimeSlots(date.toString())
            .map { docs ->
                docs.mapNotNull { it.toTimeSlot() }
                    .filter { it.remainingCapacity > 0 }
                    .sortedBy { it.startTime }
            }
    }

    override fun getAvailableSlotsForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TimeSlot>> {
        // For date range, we need to filter client-side since Firestore
        // requires a composite index for multiple inequality filters
        return firestore.listenToCollection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .map { docs ->
                docs.mapNotNull { it.toTimeSlot() }
                    .filter { slot ->
                        slot.date in startDate..endDate &&
                        slot.isAvailable &&
                        slot.remainingCapacity > 0
                    }
                    .sortedWith(compareBy({ it.date }, { it.startTime }))
            }
    }

    override suspend fun getSlotById(slotId: String): Result<TimeSlot> = runCatching {
        firestore.getTimeSlot(slotId)?.toTimeSlot()
            ?: throw Exception("Time slot not found: $slotId")
    }

    override suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?> = runCatching {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val docs = firestore.getAll(FirestoreDatabase.COLLECTION_TIME_SLOTS)

        docs.mapNotNull { it.toTimeSlot() }
            .filter { slot ->
                slot.date >= today &&
                slot.isAvailable &&
                slot.remainingCapacity > 0
            }
            .sortedWith(compareBy({ it.date }, { it.startTime }))
            .firstOrNull()
    }

    override suspend fun reserveSlot(slotId: String): Result<TimeSlot> = runCatching {
        // Get current slot data
        val slotDoc = firestore.getTimeSlot(slotId)
            ?: throw Exception("Time slot not found")

        val currentBookings = slotDoc.get<Long?>("currentBookings")?.toInt() ?: 0
        val maxCapacity = slotDoc.get<Long?>("maxCapacity")?.toInt() ?: 0

        if (currentBookings >= maxCapacity) {
            throw Exception("Time slot is fully booked")
        }

        // Update the slot
        val newBookings = currentBookings + 1
        val updates = mutableMapOf<String, Any?>(
            "currentBookings" to newBookings,
            "updatedAt" to firestore.serverTimestamp()
        )

        // Mark as unavailable if now full
        if (newBookings >= maxCapacity) {
            updates["isAvailable"] = false
        }

        firestore.updateTimeSlot(slotId, updates)

        // Return updated slot
        firestore.getTimeSlot(slotId)?.toTimeSlot()
            ?: throw Exception("Failed to retrieve updated time slot")
    }

    override suspend fun releaseSlot(slotId: String): Result<TimeSlot> = runCatching {
        // Get current slot data
        val slotDoc = firestore.getTimeSlot(slotId)
            ?: throw Exception("Time slot not found")

        val currentBookings = slotDoc.get<Long?>("currentBookings")?.toInt() ?: 0

        if (currentBookings <= 0) {
            throw Exception("No bookings to release")
        }

        // Update the slot
        val updates = mapOf<String, Any?>(
            "currentBookings" to (currentBookings - 1),
            "isAvailable" to true,
            "updatedAt" to firestore.serverTimestamp()
        )

        firestore.updateTimeSlot(slotId, updates)

        // Return updated slot
        firestore.getTimeSlot(slotId)?.toTimeSlot()
            ?: throw Exception("Failed to retrieve updated time slot")
    }

    override suspend fun syncSlots(): Result<Unit> = runCatching {
        // Firestore handles sync automatically via real-time listeners
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toTimeSlot(): TimeSlot? {
        return try {
            TimeSlot(
                id = id,
                facilityId = get("facilityId") ?: return null,
                date = LocalDate.parse(get("date") ?: return null),
                startTime = LocalTime.parse(get("startTime") ?: return null),
                endTime = LocalTime.parse(get("endTime") ?: return null),
                maxCapacity = get<Long?>("maxCapacity")?.toInt() ?: 0,
                currentBookings = get<Long?>("currentBookings")?.toInt() ?: 0,
                isAvailable = get("isAvailable") ?: true,
                slotType = try {
                    SlotType.valueOf(get("slotType") ?: "REGULAR")
                } catch (e: Exception) {
                    SlotType.REGULAR
                },
                notes = get("notes")
            )
        } catch (e: Exception) {
            null
        }
    }
}
