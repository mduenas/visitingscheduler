package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of VisitRepository.
 */
class CommonFirestoreVisitRepository(
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
        additionalVisitors: List<AdditionalVisitor>,
        videoCallLink: String?,
        videoCallPlatform: String?
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
            "videoCallLink" to videoCallLink,
            "videoCallPlatform" to videoCallPlatform,
            "createdAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        )

        val id = firestore.createVisit(visitData)
        val now = Clock.System.now()

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
            videoCallLink = videoCallLink,
            videoCallPlatform = videoCallPlatform,
            createdAt = now,
            updatedAt = now
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
            "additionalVisitors" to visit.additionalVisitors.map { av ->
                mapOf(
                    "id" to av.id,
                    "firstName" to av.firstName,
                    "lastName" to av.lastName,
                    "relationship" to av.relationship,
                    "isMinor" to av.isMinor,
                    "age" to av.age
                )
            },
            "videoCallLink" to visit.videoCallLink,
            "videoCallPlatform" to visit.videoCallPlatform,
            "updatedAt" to firestore.serverTimestamp()
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
            "cancelledAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
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
            "approvedAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
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
            "updatedAt" to firestore.serverTimestamp()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after denial")
    }

    override suspend fun checkIn(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.CHECKED_IN.name,
            "checkInTime" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after check-in")
    }

    override suspend fun checkOut(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.COMPLETED.name,
            "checkOutTime" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        )

        firestore.updateVisit(visitId, updates)

        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after check-out")
    }

    override suspend fun markAsNoShow(visitId: String): Result<Visit> = runCatching {
        val updates = mapOf<String, Any?>(
            "status" to VisitStatus.NO_SHOW.name,
            "updatedAt" to firestore.serverTimestamp()
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
            "status" to VisitStatus.PENDING.name,
            "updatedAt" to firestore.serverTimestamp()
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
                    id = map["id"] as? String ?: generateId(),
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

    private fun generateId(): String {
        return (1..20).map { ('a'..'z').random() }.joinToString("")
    }

    private fun DocumentSnapshot.toVisit(): Visit? {
        return try {
            Visit(
                id = id,
                beneficiaryId = get("beneficiaryId") ?: return null,
                visitorId = get("visitorId") ?: return null,
                scheduledDate = LocalDate.parse(get("scheduledDate") ?: return null),
                startTime = LocalTime.parse(get("startTime") ?: return null),
                endTime = LocalTime.parse(get("endTime") ?: return null),
                status = VisitStatus.valueOf(get("status") ?: "PENDING"),
                visitType = try { VisitType.valueOf(get<String>("visitType") ?: "IN_PERSON") } catch(e: Exception) { VisitType.IN_PERSON },
                purpose = get("purpose"),
                notes = get("notes"),
                additionalVisitors = parseAdditionalVisitors(get("additionalVisitors")),
                videoCallLink = get("videoCallLink"),
                videoCallPlatform = get("videoCallPlatform"),
                checkInTime = get<Long?>("checkInTime")?.let { Instant.fromEpochMilliseconds(it) },
                checkOutTime = get<Long?>("checkOutTime")?.let { Instant.fromEpochMilliseconds(it) },
                approvedBy = get("approvedBy"),
                approvedAt = get<Long?>("approvedAt")?.let { Instant.fromEpochMilliseconds(it) },
                denialReason = get("denialReason"),
                cancellationReason = get("cancellationReason"),
                cancelledBy = get("cancelledBy"),
                cancelledAt = get<Long?>("cancelledAt")?.let { Instant.fromEpochMilliseconds(it) },
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
