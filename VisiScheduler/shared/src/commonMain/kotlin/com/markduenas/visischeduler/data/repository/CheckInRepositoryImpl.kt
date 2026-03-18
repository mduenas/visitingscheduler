package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.CheckInStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * Implementation of CheckInRepository.
 */
class CheckInRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase
) : CheckInRepository {

    override suspend fun checkIn(visitId: String, method: CheckInMethod): Result<CheckIn> = runCatching {
        // Simplified: use API visit check-in and map back
        val remoteVisit = api.checkInVisit(visitId).toDomain()
        CheckIn(
            id = "ci_${remoteVisit.id}",
            visitId = remoteVisit.id,
            checkInTime = remoteVisit.checkInTime ?: Clock.System.now(),
            checkOutTime = null,
            method = method,
            notes = null,
            rating = null
        )
    }

    override suspend fun checkOut(checkInId: String, notes: String?, rating: Int?): Result<CheckIn> = runCatching {
        val visitId = checkInId.removePrefix("ci_")
        val remoteVisit = api.checkOutVisit(visitId).toDomain()
        CheckIn(
            id = checkInId,
            visitId = remoteVisit.id,
            checkInTime = remoteVisit.checkInTime ?: Clock.System.now(),
            checkOutTime = remoteVisit.checkOutTime ?: Clock.System.now(),
            method = CheckInMethod.MANUAL,
            notes = notes,
            rating = rating
        )
    }

    override suspend fun generateQrCode(visitId: String): Result<QrCodeData> = runCatching {
        val now = Clock.System.now()
        QrCodeData(
            visitId = visitId,
            visitorId = "",
            validFrom = now,
            validUntil = now,
            signature = "sig"
        )
    }

    override suspend fun validateQrCode(qrData: String): Result<QrValidationResult> = runCatching {
        val visitId = qrData.removePrefix("qrcode_")
        val visit = api.getVisitById(visitId).toDomain()
        QrValidationResult.Valid(visit)
    }

    override fun getActiveCheckIn(visitId: String): Flow<CheckIn?> = flow {
        // Try local database first
        val activeDoc = database.visiSchedulerQueries
            .selectActiveCheckInByVisitId(visitId)
            .executeAsOneOrNull()
        
        if (activeDoc != null) {
            emit(CheckIn(
                id = activeDoc.id,
                visitId = activeDoc.visit_id,
                checkInTime = Instant.fromEpochMilliseconds(activeDoc.check_in_time),
                checkOutTime = activeDoc.check_out_time?.let { Instant.fromEpochMilliseconds(it) },
                method = CheckInMethod.valueOf(activeDoc.method),
                notes = activeDoc.notes,
                rating = activeDoc.rating?.toInt()
            ))
        } else {
            emit(null)
        }
    }

    override suspend fun getCheckInById(checkInId: String): Result<CheckIn> = runCatching {
        val doc = database.visiSchedulerQueries
            .selectCheckInById(checkInId)
            .executeAsOneOrNull() ?: throw Exception("Check-in not found")
            
        CheckIn(
            id = doc.id,
            visitId = doc.visit_id,
            checkInTime = Instant.fromEpochMilliseconds(doc.check_in_time),
            checkOutTime = doc.check_out_time?.let { Instant.fromEpochMilliseconds(it) },
            method = CheckInMethod.valueOf(doc.method),
            notes = doc.notes,
            rating = doc.rating?.toInt()
        )
    }

    override fun getCheckInsForVisit(visitId: String): Flow<List<CheckIn>> = flow {
        val docs = database.visiSchedulerQueries
            .selectCheckInsByVisitId(visitId)
            .executeAsList()
            
        emit(docs.map { doc ->
            CheckIn(
                id = doc.id,
                visitId = doc.visit_id,
                checkInTime = Instant.fromEpochMilliseconds(doc.check_in_time),
                checkOutTime = doc.check_out_time?.let { Instant.fromEpochMilliseconds(it) },
                method = CheckInMethod.valueOf(doc.method),
                notes = doc.notes,
                rating = doc.rating?.toInt()
            )
        })
    }

    override fun getTodayExpectedVisitors(): Flow<List<ExpectedVisitor>> = flow {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        emitAll(getExpectedVisitorsForDate(today))
    }

    override fun getExpectedVisitorsForDate(date: LocalDate): Flow<List<ExpectedVisitor>> = flow {
        // Fetch visits for the given date from API
        val visits = try {
            api.getMyVisits().map { it.toDomain() }.filter { it.scheduledDate == date }
        } catch (e: Exception) {
            // Fallback to local database
            database.visiSchedulerQueries
                .selectVisitsInDateRange(date.toString(), date.toString())
                .executeAsList()
                .map { entity ->
                    // Simplified mapping from entity to domain
                    Visit(
                        id = entity.id,
                        beneficiaryId = entity.beneficiaryId,
                        visitorId = entity.visitorId,
                        visitorName = entity.visitorName,
                        scheduledDate = LocalDate.parse(entity.scheduledDate),
                        startTime = kotlinx.datetime.LocalTime.parse(entity.startTime),
                        endTime = kotlinx.datetime.LocalTime.parse(entity.endTime),
                        status = VisitStatus.valueOf(entity.status),
                        visitType = VisitType.valueOf(entity.visitType),
                        purpose = entity.purpose,
                        notes = entity.notes,
                        additionalVisitors = emptyList(), // Not needed for expected visitor display
                        createdAt = Instant.fromEpochMilliseconds(0),
                        updatedAt = Instant.fromEpochMilliseconds(0)
                    )
                }
        }

        val expectedVisitors = visits.map { visit ->
            // Determine status
            val status = when {
                visit.status == VisitStatus.COMPLETED -> ExpectedVisitorStatus.CHECKED_OUT
                visit.checkInTime != null && visit.checkOutTime == null -> ExpectedVisitorStatus.CHECKED_IN
                visit.status == VisitStatus.NO_SHOW -> ExpectedVisitorStatus.NO_SHOW
                // Add logic for LATE if needed
                else -> ExpectedVisitorStatus.NOT_ARRIVED
            }

            ExpectedVisitor(
                visit = visit,
                visitorName = visit.visitorName,
                visitorPhotoUrl = null, // In a real app, fetch from User
                beneficiaryName = "Beneficiary", // In a real app, fetch from Beneficiary
                beneficiaryRoom = null,
                checkInStatus = status
            )
        }
        
        emit(expectedVisitors)
    }

    override suspend fun generateVisitorBadge(checkInId: String): Result<VisitorBadge> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun verifyBadge(badgeQrData: String): Result<VisitorBadge> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun getCheckInStatistics(startDate: LocalDate, endDate: LocalDate): Result<CheckInStatistics> = runCatching {
        CheckInStatistics(0, 0, 0, 0, 0, 0f, 0f, 0f, 0)
    }

    override suspend fun syncCheckIns(): Result<Unit> = runCatching {
        Result.success(Unit)
    }
}
