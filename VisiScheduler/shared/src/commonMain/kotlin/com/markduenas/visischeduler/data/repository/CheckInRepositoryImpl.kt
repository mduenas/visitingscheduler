package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.CheckInRequestDto
import com.markduenas.visischeduler.data.remote.dto.CheckOutRequestDto
import com.markduenas.visischeduler.data.remote.dto.ValidateQrRequestDto
import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.QrValidationResult
import com.markduenas.visischeduler.domain.entities.VisitorBadge
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.CheckInStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Implementation of CheckInRepository.
 */
class CheckInRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase
) : CheckInRepository {

    override suspend fun checkIn(visitId: String, method: CheckInMethod): Result<CheckIn> {
        return withContext(Dispatchers.Default) {
            try {
                val request = CheckInRequestDto(method = method.name)
                val response = api.checkInVisit(visitId, request)
                val checkIn = response.toDomain()

                // Cache locally
                cacheCheckIn(checkIn)

                Result.success(checkIn)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun checkOut(checkInId: String, notes: String?, rating: Int?): Result<CheckIn> {
        return withContext(Dispatchers.Default) {
            try {
                val request = CheckOutRequestDto(notes = notes, rating = rating)
                val response = api.checkOutFromCheckIn(checkInId, request)
                val checkIn = response.toDomain()

                // Update local cache
                cacheCheckIn(checkIn)

                Result.success(checkIn)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun generateQrCode(visitId: String): Result<QrCodeData> {
        return withContext(Dispatchers.Default) {
            try {
                val response = api.generateQrCode(visitId)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun validateQrCode(qrData: String): Result<QrValidationResult> {
        return withContext(Dispatchers.Default) {
            try {
                val request = ValidateQrRequestDto(qrData = qrData)
                val response = api.validateQrCode(request)

                val result = when (response.status) {
                    "VALID" -> {
                        QrValidationResult.Valid(response.visit!!.toDomain())
                    }
                    "EXPIRED" -> {
                        QrValidationResult.Expired(Instant.parse(response.expiredAt!!))
                    }
                    "NOT_YET_VALID" -> {
                        QrValidationResult.NotYetValid(Instant.parse(response.validFrom!!))
                    }
                    "INVALID_SIGNATURE" -> {
                        QrValidationResult.InvalidSignature(response.message ?: "Invalid signature")
                    }
                    "VISIT_NOT_FOUND" -> {
                        QrValidationResult.VisitNotFound(response.message ?: "Unknown visit")
                    }
                    "ALREADY_CHECKED_IN" -> {
                        QrValidationResult.AlreadyCheckedIn(response.checkIn!!.toDomain())
                    }
                    "VISIT_CANCELLED" -> {
                        QrValidationResult.VisitCancelled(response.visit!!.toDomain())
                    }
                    else -> {
                        QrValidationResult.InvalidSignature("Unknown validation status: ${response.status}")
                    }
                }

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getActiveCheckIn(visitId: String): Flow<CheckIn?> = flow {
        // First try local cache
        val localCheckIn = getActiveCheckInFromCache(visitId)
        emit(localCheckIn)

        // Then fetch from remote
        try {
            val remoteCheckIn = api.getActiveCheckIn(visitId)?.toDomain()
            if (remoteCheckIn != null) {
                cacheCheckIn(remoteCheckIn)
            }
            emit(remoteCheckIn)
        } catch (e: Exception) {
            // Keep emitting cached value if network fails
        }
    }

    override suspend fun getCheckInById(checkInId: String): Result<CheckIn> {
        return withContext(Dispatchers.Default) {
            try {
                // Try local first
                val localCheckIn = getCheckInFromCache(checkInId)
                if (localCheckIn != null) {
                    return@withContext Result.success(localCheckIn)
                }

                // Fetch from remote
                val response = api.getCheckInById(checkInId)
                val checkIn = response.toDomain()
                cacheCheckIn(checkIn)
                Result.success(checkIn)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun getCheckInsForVisit(visitId: String): Flow<List<CheckIn>> = flow {
        // First emit from cache
        val cachedCheckIns = getCheckInsFromCache(visitId)
        emit(cachedCheckIns)

        // Then fetch from remote
        try {
            val remoteCheckIns = api.getCheckInsForVisit(visitId).map { it.toDomain() }
            remoteCheckIns.forEach { cacheCheckIn(it) }
            emit(remoteCheckIns)
        } catch (e: Exception) {
            // Keep emitting cached values if network fails
        }
    }

    override fun getTodayExpectedVisitors(): Flow<List<ExpectedVisitor>> = flow {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        getExpectedVisitorsForDate(today).collect { emit(it) }
    }

    override fun getExpectedVisitorsForDate(date: LocalDate): Flow<List<ExpectedVisitor>> = flow {
        try {
            val visitors = api.getExpectedVisitors(date.toString()).map { it.toDomain() }
            emit(visitors)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun generateVisitorBadge(checkInId: String): Result<VisitorBadge> {
        return withContext(Dispatchers.Default) {
            try {
                val response = api.generateVisitorBadge(checkInId)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun verifyBadge(badgeQrData: String): Result<VisitorBadge> {
        return withContext(Dispatchers.Default) {
            try {
                val response = api.verifyBadge(ValidateQrRequestDto(badgeQrData))
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCheckInStatistics(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<CheckInStatistics> {
        return withContext(Dispatchers.Default) {
            try {
                val response = api.getCheckInStatistics(startDate.toString(), endDate.toString())
                Result.success(
                    CheckInStatistics(
                        totalCheckIns = response.totalCheckIns,
                        qrCodeCheckIns = response.qrCodeCheckIns,
                        manualCheckIns = response.manualCheckIns,
                        automaticCheckIns = response.automaticCheckIns,
                        averageVisitDurationMinutes = response.averageVisitDurationMinutes,
                        averageRating = response.averageRating,
                        onTimePercentage = response.onTimePercentage,
                        latePercentage = response.latePercentage,
                        noShowCount = response.noShowCount
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncCheckIns(): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                // Get recent check-ins from server and cache them
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val checkIns = api.getRecentCheckIns(today.toString())
                checkIns.forEach { cacheCheckIn(it.toDomain()) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Local cache operations
    private suspend fun cacheCheckIn(checkIn: CheckIn) {
        database.checkInQueries.insertCheckIn(
            id = checkIn.id,
            visit_id = checkIn.visitId,
            check_in_time = checkIn.checkInTime.toEpochMilliseconds(),
            check_out_time = checkIn.checkOutTime?.toEpochMilliseconds(),
            method = checkIn.method.name,
            notes = checkIn.notes,
            rating = checkIn.rating?.toLong()
        )
    }

    private fun getCheckInFromCache(checkInId: String): CheckIn? {
        return database.checkInQueries.selectCheckInById(checkInId).executeAsOneOrNull()?.let {
            CheckIn(
                id = it.id,
                visitId = it.visit_id,
                checkInTime = Instant.fromEpochMilliseconds(it.check_in_time),
                checkOutTime = it.check_out_time?.let { time -> Instant.fromEpochMilliseconds(time) },
                method = CheckInMethod.valueOf(it.method),
                notes = it.notes,
                rating = it.rating?.toInt()
            )
        }
    }

    private fun getActiveCheckInFromCache(visitId: String): CheckIn? {
        return database.checkInQueries.selectActiveCheckInByVisitId(visitId).executeAsOneOrNull()?.let {
            CheckIn(
                id = it.id,
                visitId = it.visit_id,
                checkInTime = Instant.fromEpochMilliseconds(it.check_in_time),
                checkOutTime = null,
                method = CheckInMethod.valueOf(it.method),
                notes = it.notes,
                rating = it.rating?.toInt()
            )
        }
    }

    private fun getCheckInsFromCache(visitId: String): List<CheckIn> {
        return database.checkInQueries.selectCheckInsByVisitId(visitId).executeAsList().map {
            CheckIn(
                id = it.id,
                visitId = it.visit_id,
                checkInTime = Instant.fromEpochMilliseconds(it.check_in_time),
                checkOutTime = it.check_out_time?.let { time -> Instant.fromEpochMilliseconds(time) },
                method = CheckInMethod.valueOf(it.method),
                notes = it.notes,
                rating = it.rating?.toInt()
            )
        }
    }
}
