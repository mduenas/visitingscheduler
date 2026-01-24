package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.AdditionalVisitorDto
import com.markduenas.visischeduler.data.remote.dto.VisitRequestDto
import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of VisitRepository.
 */
class VisitRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : VisitRepository {

    override fun getMyVisits(): Flow<List<Visit>> = flow {
        // First emit cached data
        val cached = getCachedVisits()
        emit(cached)

        // Then fetch from API and update
        try {
            val visits = api.getMyVisits().map { it.toDomain() }
            visits.forEach { cacheVisit(it) }
            emit(visits)
        } catch (e: Exception) {
            // Keep cached data on error
        }
    }

    override fun getMyVisitsByStatus(status: VisitStatus): Flow<List<Visit>> = flow {
        val visits = getCachedVisitsByStatus(status)
        emit(visits)

        try {
            val remoteVisits = api.getMyVisits()
                .map { it.toDomain() }
                .filter { it.status == status }
            remoteVisits.forEach { cacheVisit(it) }
            emit(remoteVisits)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override fun getUpcomingVisits(): Flow<List<Visit>> = flow {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val cached = database.visiSchedulerQueries
            .selectUpcomingVisits(today.toString())
            .executeAsList()
            .map { mapEntityToVisit(it) }
        emit(cached)

        try {
            val visits = api.getMyVisits()
                .map { it.toDomain() }
                .filter { it.isUpcoming }
            visits.forEach { cacheVisit(it) }
            emit(visits)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override fun getPastVisits(): Flow<List<Visit>> = flow {
        val cached = database.visiSchedulerQueries
            .selectPastVisits()
            .executeAsList()
            .map { mapEntityToVisit(it) }
        emit(cached)

        try {
            val visits = api.getMyVisits()
                .map { it.toDomain() }
                .filter { it.isPast }
            visits.forEach { cacheVisit(it) }
            emit(visits)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override suspend fun getVisitById(visitId: String): Result<Visit> {
        return try {
            val visit = api.getVisitById(visitId).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            // Try cache
            val cached = database.visiSchedulerQueries
                .selectVisitById(visitId)
                .executeAsOneOrNull()
            if (cached != null) {
                Result.success(mapEntityToVisit(cached))
            } else {
                Result.failure(e)
            }
        }
    }

    override fun getVisitsForBeneficiary(beneficiaryId: String): Flow<List<Visit>> = flow {
        val cached = database.visiSchedulerQueries
            .selectVisitsByBeneficiaryId(beneficiaryId)
            .executeAsList()
            .map { mapEntityToVisit(it) }
        emit(cached)

        try {
            val visits = api.getVisitsForBeneficiary(beneficiaryId).map { it.toDomain() }
            visits.forEach { cacheVisit(it) }
            emit(visits)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override fun getPendingApprovalVisits(): Flow<List<Visit>> = flow {
        val cached = database.visiSchedulerQueries
            .selectPendingApprovalVisits()
            .executeAsList()
            .map { mapEntityToVisit(it) }
        emit(cached)

        try {
            val visits = api.getPendingApprovalVisits().map { it.toDomain() }
            visits.forEach { cacheVisit(it) }
            emit(visits)
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override fun getVisitsInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Visit>> = flow {
        val cached = database.visiSchedulerQueries
            .selectVisitsInDateRange(startDate.toString(), endDate.toString())
            .executeAsList()
            .map { mapEntityToVisit(it) }
        emit(cached)
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
    ): Result<Visit> {
        return try {
            val request = VisitRequestDto(
                beneficiaryId = beneficiaryId,
                scheduledDate = scheduledDate.toString(),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                visitType = visitType.name,
                purpose = purpose,
                notes = notes,
                additionalVisitors = additionalVisitors.map { AdditionalVisitorDto.fromDomain(it) }
            )
            val visit = api.scheduleVisit(request).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVisit(visit: Visit): Result<Visit> {
        return try {
            val request = VisitRequestDto(
                beneficiaryId = visit.beneficiaryId,
                scheduledDate = visit.scheduledDate.toString(),
                startTime = visit.startTime.toString(),
                endTime = visit.endTime.toString(),
                visitType = visit.visitType.name,
                purpose = visit.purpose,
                notes = visit.notes,
                additionalVisitors = visit.additionalVisitors.map { AdditionalVisitorDto.fromDomain(it) }
            )
            val updated = api.updateVisit(visit.id, request).toDomain()
            cacheVisit(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelVisit(visitId: String, reason: String): Result<Visit> {
        return try {
            val visit = api.cancelVisit(visitId, reason).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveVisit(visitId: String, notes: String?): Result<Visit> {
        return try {
            val visit = api.approveVisit(visitId, notes).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyVisit(visitId: String, reason: String): Result<Visit> {
        return try {
            val visit = api.denyVisit(visitId, reason).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkIn(visitId: String): Result<Visit> {
        return try {
            val visit = api.checkInVisit(visitId).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkOut(visitId: String): Result<Visit> {
        return try {
            val visit = api.checkOutVisit(visitId).toDomain()
            cacheVisit(visit)
            Result.success(visit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsNoShow(visitId: String): Result<Visit> {
        return try {
            // This would be an API call in real implementation
            val visitResult = getVisitById(visitId)
            if (visitResult.isFailure) return visitResult

            val visit = visitResult.getOrNull()!!
            val updated = visit.copy(status = VisitStatus.NO_SHOW)
            cacheVisit(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleVisit(
        visitId: String,
        newDate: LocalDate,
        newStartTime: LocalTime,
        newEndTime: LocalTime
    ): Result<Visit> {
        return try {
            val visitResult = getVisitById(visitId)
            if (visitResult.isFailure) return visitResult

            val visit = visitResult.getOrNull()!!
            val request = VisitRequestDto(
                beneficiaryId = visit.beneficiaryId,
                scheduledDate = newDate.toString(),
                startTime = newStartTime.toString(),
                endTime = newEndTime.toString(),
                visitType = visit.visitType.name,
                purpose = visit.purpose,
                notes = visit.notes,
                additionalVisitors = visit.additionalVisitors.map { AdditionalVisitorDto.fromDomain(it) }
            )
            val updated = api.updateVisit(visitId, request).toDomain()
            cacheVisit(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getVisitStatistics(): Result<VisitStatistics> {
        return try {
            val visits = getCachedVisits()
            val stats = VisitStatistics(
                totalVisits = visits.size,
                completedVisits = visits.count { it.status == VisitStatus.COMPLETED },
                cancelledVisits = visits.count { it.status == VisitStatus.CANCELLED },
                noShowVisits = visits.count { it.status == VisitStatus.NO_SHOW },
                upcomingVisits = visits.count { it.status == VisitStatus.APPROVED },
                pendingVisits = visits.count { it.status == VisitStatus.PENDING }
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncVisits(): Result<Unit> {
        return try {
            val visits = api.getMyVisits().map { it.toDomain() }
            database.visiSchedulerQueries.deleteAllVisits()
            visits.forEach { cacheVisit(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCachedVisits(): List<Visit> {
        // Note: This would need a proper query in production
        return emptyList()
    }

    private fun getCachedVisitsByStatus(status: VisitStatus): List<Visit> {
        return database.visiSchedulerQueries
            .selectVisitsByStatus(status.name)
            .executeAsList()
            .map { mapEntityToVisit(it) }
    }

    private fun cacheVisit(visit: Visit) {
        database.visiSchedulerQueries.insertVisit(
            id = visit.id,
            beneficiaryId = visit.beneficiaryId,
            visitorId = visit.visitorId,
            scheduledDate = visit.scheduledDate.toString(),
            startTime = visit.startTime.toString(),
            endTime = visit.endTime.toString(),
            status = visit.status.name,
            visitType = visit.visitType.name,
            purpose = visit.purpose,
            notes = visit.notes,
            additionalVisitors = json.encodeToString(visit.additionalVisitors),
            checkInTime = visit.checkInTime?.toString(),
            checkOutTime = visit.checkOutTime?.toString(),
            approvedBy = visit.approvedBy,
            approvedAt = visit.approvedAt?.toString(),
            denialReason = visit.denialReason,
            cancellationReason = visit.cancellationReason,
            cancelledBy = visit.cancelledBy,
            cancelledAt = visit.cancelledAt?.toString(),
            createdAt = visit.createdAt.toString(),
            updatedAt = visit.updatedAt.toString()
        )
    }

    private fun mapEntityToVisit(entity: com.markduenas.visischeduler.data.local.VisitEntity): Visit {
        return Visit(
            id = entity.id,
            beneficiaryId = entity.beneficiaryId,
            visitorId = entity.visitorId,
            scheduledDate = LocalDate.parse(entity.scheduledDate),
            startTime = LocalTime.parse(entity.startTime),
            endTime = LocalTime.parse(entity.endTime),
            status = VisitStatus.valueOf(entity.status),
            visitType = VisitType.valueOf(entity.visitType),
            purpose = entity.purpose,
            notes = entity.notes,
            additionalVisitors = json.decodeFromString(entity.additionalVisitors),
            checkInTime = entity.checkInTime?.let { kotlinx.datetime.Instant.parse(it) },
            checkOutTime = entity.checkOutTime?.let { kotlinx.datetime.Instant.parse(it) },
            approvedBy = entity.approvedBy,
            approvedAt = entity.approvedAt?.let { kotlinx.datetime.Instant.parse(it) },
            denialReason = entity.denialReason,
            cancellationReason = entity.cancellationReason,
            cancelledBy = entity.cancelledBy,
            cancelledAt = entity.cancelledAt?.let { kotlinx.datetime.Instant.parse(it) },
            createdAt = kotlinx.datetime.Instant.parse(entity.createdAt),
            updatedAt = kotlinx.datetime.Instant.parse(entity.updatedAt)
        )
    }
}
