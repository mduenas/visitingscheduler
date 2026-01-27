package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Firestore implementation of TimeSlotRepository.
 * Manages time slot availability for visit scheduling.
 */
class FirestoreTimeSlotRepository(
    private val firestore: FirestoreDatabase
) : TimeSlotRepository {

    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    override fun getAvailableSlotsForDate(
        beneficiaryId: String,
        date: LocalDate
    ): Flow<List<TimeSlot>> = callbackFlow {
        val listener = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .whereEqualTo("date", date.toString())
            .whereEqualTo("isAvailable", true)
            .whereGreaterThan("remainingCapacity", 0)
            .orderBy("remainingCapacity", Query.Direction.DESCENDING)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val slots = snapshot?.documents?.mapNotNull { it.toTimeSlot() } ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override fun getAvailableSlotsForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TimeSlot>> = callbackFlow {
        val listener = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .whereGreaterThanOrEqualTo("date", startDate.toString())
            .whereLessThanOrEqualTo("date", endDate.toString())
            .whereEqualTo("isAvailable", true)
            .orderBy("date", Query.Direction.ASCENDING)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val slots = snapshot?.documents?.mapNotNull { it.toTimeSlot() }
                    ?.filter { it.remainingCapacity > 0 } ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getSlotById(slotId: String): Result<TimeSlot> = runCatching {
        val doc = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .document(slotId)
            .get()
            .await()

        doc.toTimeSlot() ?: throw Exception("Time slot not found: $slotId")
    }

    override suspend fun getNextAvailableSlot(beneficiaryId: String): Result<TimeSlot?> = runCatching {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val docs = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .whereGreaterThanOrEqualTo("date", today.toString())
            .whereEqualTo("isAvailable", true)
            .orderBy("date", Query.Direction.ASCENDING)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .await()
            .documents

        // Find first slot with remaining capacity
        docs.mapNotNull { it.toTimeSlot() }
            .firstOrNull { it.remainingCapacity > 0 }
    }

    override suspend fun reserveSlot(slotId: String): Result<TimeSlot> = runCatching {
        val slotRef = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS).document(slotId)

        // Use transaction to ensure atomic update
        db.runTransaction { transaction ->
            val snapshot = transaction.get(slotRef)
            val currentBookings = snapshot.getLong("currentBookings")?.toInt() ?: 0
            val maxCapacity = snapshot.getLong("maxCapacity")?.toInt() ?: 0

            if (currentBookings >= maxCapacity) {
                throw Exception("Time slot is fully booked")
            }

            transaction.update(slotRef, mapOf(
                "currentBookings" to FieldValue.increment(1),
                "updatedAt" to Timestamp.now()
            ))

            // Update isAvailable if this booking fills the slot
            if (currentBookings + 1 >= maxCapacity) {
                transaction.update(slotRef, "isAvailable", false)
            }
        }.await()

        // Return updated slot
        slotRef.get().await().toTimeSlot()
            ?: throw Exception("Failed to retrieve updated time slot")
    }

    override suspend fun releaseSlot(slotId: String): Result<TimeSlot> = runCatching {
        val slotRef = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS).document(slotId)

        // Use transaction to ensure atomic update
        db.runTransaction { transaction ->
            val snapshot = transaction.get(slotRef)
            val currentBookings = snapshot.getLong("currentBookings")?.toInt() ?: 0

            if (currentBookings <= 0) {
                throw Exception("No bookings to release")
            }

            transaction.update(slotRef, mapOf(
                "currentBookings" to FieldValue.increment(-1),
                "isAvailable" to true,
                "updatedAt" to Timestamp.now()
            ))
        }.await()

        // Return updated slot
        slotRef.get().await().toTimeSlot()
            ?: throw Exception("Failed to retrieve updated time slot")
    }

    override suspend fun syncSlots(): Result<Unit> = runCatching {
        // Firestore handles sync automatically via real-time listeners
        // This could trigger a refresh or generate default slots if needed
    }

    /**
     * Create a new time slot (for coordinators/admins).
     */
    suspend fun createSlot(
        facilityId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        maxCapacity: Int,
        slotType: SlotType = SlotType.REGULAR,
        notes: String? = null
    ): Result<TimeSlot> = runCatching {
        val slotData = mapOf<String, Any?>(
            "facilityId" to facilityId,
            "date" to date.toString(),
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString(),
            "maxCapacity" to maxCapacity,
            "currentBookings" to 0,
            "isAvailable" to true,
            "slotType" to slotType.name,
            "notes" to notes,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        ).filterValues { it != null }

        val docRef = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .add(slotData)
            .await()

        TimeSlot(
            id = docRef.id,
            facilityId = facilityId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            maxCapacity = maxCapacity,
            currentBookings = 0,
            isAvailable = true,
            slotType = slotType,
            notes = notes
        )
    }

    /**
     * Update a time slot.
     */
    suspend fun updateSlot(
        slotId: String,
        maxCapacity: Int? = null,
        isAvailable: Boolean? = null,
        notes: String? = null
    ): Result<TimeSlot> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "updatedAt" to Timestamp.now()
        )
        maxCapacity?.let { updates["maxCapacity"] = it }
        isAvailable?.let { updates["isAvailable"] = it }
        notes?.let { updates["notes"] = it }

        db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .document(slotId)
            .update(updates.filterValues { it != null }.mapValues { it.value!! })
            .await()

        getSlotById(slotId).getOrThrow()
    }

    /**
     * Delete a time slot.
     */
    suspend fun deleteSlot(slotId: String): Result<Unit> = runCatching {
        db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .document(slotId)
            .delete()
            .await()
    }

    /**
     * Get all slots for a facility on a specific date (includes unavailable).
     */
    fun getAllSlotsForDate(facilityId: String, date: LocalDate): Flow<List<TimeSlot>> = callbackFlow {
        val listener = db.collection(FirestoreDatabase.COLLECTION_TIME_SLOTS)
            .whereEqualTo("facilityId", facilityId)
            .whereEqualTo("date", date.toString())
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val slots = snapshot?.documents?.mapNotNull { it.toTimeSlot() } ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toTimeSlot(): TimeSlot? {
        return try {
            TimeSlot(
                id = id,
                facilityId = getString("facilityId") ?: return null,
                date = LocalDate.parse(getString("date") ?: return null),
                startTime = LocalTime.parse(getString("startTime") ?: return null),
                endTime = LocalTime.parse(getString("endTime") ?: return null),
                maxCapacity = getLong("maxCapacity")?.toInt() ?: 0,
                currentBookings = getLong("currentBookings")?.toInt() ?: 0,
                isAvailable = getBoolean("isAvailable") ?: true,
                slotType = try {
                    SlotType.valueOf(getString("slotType") ?: "REGULAR")
                } catch (e: Exception) {
                    SlotType.REGULAR
                },
                notes = getString("notes")
            )
        } catch (e: Exception) {
            null
        }
    }
}
