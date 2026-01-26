package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Firestore implementation of VisitRepository.
 * Uses Firebase Firestore as the backend database.
 */
class FirestoreVisitRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : VisitRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun getMyVisits(): Flow<List<Visit>> {
        val userId = currentUserId ?: return flowOf(emptyList())
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_VISITS,
            "visitorId",
            userId,
            orderBy = "scheduledDate"
        ).map { docs -> docs.mapNotNull { it.toVisit() } }
    }

    override fun getMyVisitsByStatus(status: VisitStatus): Flow<List<Visit>> {
        return getMyVisits().map { visits ->
            visits.filter { it.status == status }
        }
    }

    override fun getUpcomingVisits(): Flow<List<Visit>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return getMyVisits().map { visits ->
            visits.filter { visit ->
                visit.scheduledDate >= today &&
                visit.status in listOf(VisitStatus.PENDING, VisitStatus.APPROVED)
            }
        }
    }

    override fun getPastVisits(): Flow<List<Visit>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return getMyVisits().map { visits ->
            visits.filter { visit ->
                visit.scheduledDate < today ||
                visit.status in listOf(VisitStatus.COMPLETED, VisitStatus.CANCELLED, VisitStatus.NO_SHOW)
            }
        }
    }

    override suspend fun getVisitById(visitId: String): Result<Visit> = runCatching {
        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found")
    }

    override fun getVisitsForBeneficiary(beneficiaryId: String): Flow<List<Visit>> {
        return firestore.listenToVisitsForBeneficiary(beneficiaryId)
            .map { docs -> docs.mapNotNull { it.toVisit() } }
    }

    override fun getPendingApprovalVisits(): Flow<List<Visit>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_VISITS,
            "status",
            VisitStatus.PENDING.name,
            orderBy = "scheduledDate"
        ).map { docs -> docs.mapNotNull { it.toVisit() } }
    }

    override fun getVisitsInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Visit>> {
        val userId = currentUserId ?: return flowOf(emptyList())
        return getMyVisits().map { visits ->
            visits.filter { visit ->
                visit.scheduledDate in startDate..endDate
            }
        }
    }

    override suspend fun scheduleVisit(
        beneficiaryId: String,
        scheduledDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        visitType: VisitType,
        purpose: String?,
        notes: String?,
        additionalVisitors: List<AdditionalVisitor>
    ): Result<Visit> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val visitData = mapOf<String, Any?>(
            "beneficiaryId" to beneficiaryId,
            "visitorId" to userId,
            "scheduledDate" to scheduledDate.toString(),
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString(),
            "status" to VisitStatus.PENDING.name,
            "visitType" to visitType.name,
            "purpose" to purpose,
            "notes" to notes,
            "additionalVisitors" to additionalVisitors.map { av ->
                mapOf(
                    "id" to av.id,
                    "firstName" to av.firstName,
                    "lastName" to av.lastName,
                    "relationship" to av.relationship,
                    "isMinor" to av.isMinor,
                    "age" to av.age
                )
            },
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        val id = firestore.createVisit(visitData)

        Visit(
            id = id,
            beneficiaryId = beneficiaryId,
            visitorId = userId,
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
            status = VisitStatus.PENDING,
            visitType = visitType,
            purpose = purpose,
            notes = notes,
            additionalVisitors = additionalVisitors,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
    }

    override suspend fun updateVisit(visit: Visit): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "scheduledDate" to visit.scheduledDate.toString(),
            "startTime" to visit.startTime.toString(),
            "endTime" to visit.endTime.toString(),
            "visitType" to visit.visitType.name,
            "purpose" to visit.purpose,
            "notes" to visit.notes,
            "additionalVisitors" to visit.additionalVisitors,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visit.id, updates)
        visit.copy(updatedAt = Clock.System.now())
    }

    override suspend fun cancelVisit(visitId: String, reason: String): Result<Visit> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.CANCELLED.name,
            "cancelledBy" to userId,
            "cancellationReason" to reason,
            "cancelledAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after cancellation")
    }

    override suspend fun approveVisit(visitId: String, notes: String?): Result<Visit> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val updates = mutableMapOf<String, Any?>(
            "status" to VisitStatus.APPROVED.name,
            "approvedBy" to userId,
            "approvedAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        notes?.let { updates["approvalNotes"] = it }

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after approval")
    }

    override suspend fun denyVisit(visitId: String, reason: String): Result<Visit> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")

        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.DENIED.name,
            "deniedBy" to userId,
            "denialReason" to reason,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after denial")
    }

    override suspend fun checkIn(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.CHECKED_IN.name,
            "checkInTime" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after check-in")
    }

    override suspend fun checkOut(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.COMPLETED.name,
            "checkOutTime" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after check-out")
    }

    override suspend fun markAsNoShow(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.NO_SHOW.name,
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after marking as no-show")
    }

    override suspend fun rescheduleVisit(
        visitId: String,
        newDate: LocalDate,
        newStartTime: LocalTime,
        newEndTime: LocalTime
    ): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "scheduledDate" to newDate.toString(),
            "startTime" to newStartTime.toString(),
            "endTime" to newEndTime.toString(),
            "status" to VisitStatus.PENDING.name, // Requires re-approval
            "updatedAt" to Timestamp.now()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after rescheduling")
    }

    override suspend fun getVisitStatistics(): Result<VisitStatistics> = runCatching {
        val userId = currentUserId ?: throw Exception("User not authenticated")
        val allVisits = firestore.getVisitsForVisitor(userId).mapNotNull { it.toVisit() }

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        VisitStatistics(
            totalVisits = allVisits.size,
            completedVisits = allVisits.count { it.status == VisitStatus.COMPLETED },
            cancelledVisits = allVisits.count { it.status == VisitStatus.CANCELLED },
            noShowVisits = allVisits.count { it.status == VisitStatus.NO_SHOW },
            upcomingVisits = allVisits.count {
                it.scheduledDate >= today && it.status in listOf(VisitStatus.PENDING, VisitStatus.APPROVED)
            },
            pendingVisits = allVisits.count { it.status == VisitStatus.PENDING }
        )
    }

    override suspend fun syncVisits(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun parseAdditionalVisitors(data: Any?): List<AdditionalVisitor> {
        val list = data as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            try {
                AdditionalVisitor(
                    id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                    firstName = map["firstName"] as? String ?: "",
                    lastName = map["lastName"] as? String ?: "",
                    relationship = map["relationship"] as? String ?: "",
                    isMinor = map["isMinor"] as? Boolean ?: false,
                    age = (map["age"] as? Number)?.toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun DocumentSnapshot.toVisit(): Visit? {
        return try {
            Visit(
                id = id,
                beneficiaryId = getString("beneficiaryId") ?: return null,
                visitorId = getString("visitorId") ?: return null,
                scheduledDate = LocalDate.parse(getString("scheduledDate") ?: return null),
                startTime = LocalTime.parse(getString("startTime") ?: return null),
                endTime = LocalTime.parse(getString("endTime") ?: return null),
                status = VisitStatus.valueOf(getString("status") ?: "PENDING"),
                visitType = VisitType.valueOf(getString("visitType") ?: "IN_PERSON"),
                purpose = getString("purpose"),
                notes = getString("notes"),
                additionalVisitors = parseAdditionalVisitors(get("additionalVisitors")),
                checkInTime = getTimestamp("checkInTime")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                checkOutTime = getTimestamp("checkOutTime")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                approvedBy = getString("approvedBy"),
                approvedAt = getTimestamp("approvedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                denialReason = getString("denialReason"),
                cancellationReason = getString("cancellationReason"),
                cancelledBy = getString("cancelledBy"),
                cancelledAt = getTimestamp("cancelledAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
