package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Firestore implementation of VisitRepository.
 * Uses Firebase Firestore as the backend database.
 */
class FirestoreVisitRepository(
    private val firestore: FirestoreDatabase
) : VisitRepository {

    override suspend fun createVisit(visit: Visit): Result<Visit> = runCatching {
        val visitData = visit.toFirestoreMap()
        val id = firestore.createVisit(visitData)
        visit.copy(id = id)
    }

    override suspend fun getVisit(visitId: String): Result<Visit?> = runCatching {
        firestore.getVisit(visitId)?.toVisit()
    }

    override suspend fun updateVisit(visit: Visit): Result<Visit> = runCatching {
        firestore.updateVisit(visit.id, visit.toFirestoreMap())
        visit
    }

    override suspend fun deleteVisit(visitId: String): Result<Unit> = runCatching {
        firestore.delete(FirestoreDatabase.COLLECTION_VISITS, visitId)
    }

    override suspend fun getVisitsForBeneficiary(beneficiaryId: String): Result<List<Visit>> = runCatching {
        firestore.getVisitsForBeneficiary(beneficiaryId).mapNotNull { it.toVisit() }
    }

    override suspend fun getVisitsForVisitor(visitorId: String): Result<List<Visit>> = runCatching {
        firestore.getVisitsForVisitor(visitorId).mapNotNull { it.toVisit() }
    }

    override suspend fun getVisitsByStatus(status: VisitStatus): Result<List<Visit>> = runCatching {
        firestore.getVisitsByStatus(status.name).mapNotNull { it.toVisit() }
    }

    override suspend fun getVisitsForDate(
        beneficiaryId: String,
        date: LocalDate
    ): Result<List<Visit>> = runCatching {
        val visits = firestore.getVisitsForBeneficiary(beneficiaryId)
        visits.mapNotNull { it.toVisit() }
            .filter { it.scheduledDate == date }
    }

    override suspend fun getVisitsForDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Visit>> = runCatching {
        val visits = firestore.getVisitsForBeneficiary(beneficiaryId)
        visits.mapNotNull { it.toVisit() }
            .filter { it.scheduledDate in startDate..endDate }
    }

    override suspend fun getPendingVisits(beneficiaryId: String): Result<List<Visit>> = runCatching {
        val visits = firestore.getVisitsForBeneficiary(beneficiaryId)
        visits.mapNotNull { it.toVisit() }
            .filter { it.status == VisitStatus.PENDING }
    }

    override suspend fun approveVisit(
        visitId: String,
        approverId: String
    ): Result<Visit> = runCatching {
        val updates = mapOf(
            "status" to VisitStatus.APPROVED.name,
            "approvedBy" to approverId,
            "approvedAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateVisit(visitId, updates)
        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after approval")
    }

    override suspend fun denyVisit(
        visitId: String,
        approverId: String,
        reason: String?
    ): Result<Visit> = runCatching {
        val updates = mapOf(
            "status" to VisitStatus.DENIED.name,
            "deniedBy" to approverId,
            "denialReason" to (reason ?: ""),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateVisit(visitId, updates)
        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after denial")
    }

    override suspend fun cancelVisit(
        visitId: String,
        cancelledBy: String,
        reason: String?
    ): Result<Visit> = runCatching {
        val updates = mapOf(
            "status" to VisitStatus.CANCELLED.name,
            "cancelledBy" to cancelledBy,
            "cancellationReason" to (reason ?: ""),
            "cancelledAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        firestore.updateVisit(visitId, updates)
        firestore.getVisit(visitId)?.toVisit()
            ?: throw Exception("Visit not found after cancellation")
    }

    override fun observeVisitsForBeneficiary(beneficiaryId: String): Flow<List<Visit>> {
        return firestore.listenToVisitsForBeneficiary(beneficiaryId)
            .map { docs -> docs.mapNotNull { it.toVisit() } }
    }

    override fun observeVisit(visitId: String): Flow<Visit?> {
        return firestore.listenToDocument(FirestoreDatabase.COLLECTION_VISITS, visitId)
            .map { it?.toVisit() }
    }

    // ==================== Mapping Functions ====================

    private fun Visit.toFirestoreMap(): Map<String, Any?> = mapOf(
        "beneficiaryId" to beneficiaryId,
        "visitorId" to visitorId,
        "scheduledDate" to scheduledDate.toString(),
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "status" to status.name,
        "visitType" to visitType.name,
        "purpose" to purpose,
        "notes" to notes,
        "additionalVisitors" to additionalVisitors,
        "approvedBy" to approvedBy,
        "denialReason" to denialReason,
        "cancellationReason" to cancellationReason,
        "cancelledBy" to cancelledBy,
        "createdAt" to Timestamp.now(),
        "updatedAt" to Timestamp.now()
    )

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
                additionalVisitors = (get("additionalVisitors") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                checkInTime = getString("checkInTime")?.let { Instant.parse(it) },
                checkOutTime = getString("checkOutTime")?.let { Instant.parse(it) },
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
