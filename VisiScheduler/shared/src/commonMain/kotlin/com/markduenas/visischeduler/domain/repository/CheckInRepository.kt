package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.QrValidationResult
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitorBadge
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for check-in/check-out operations.
 */
interface CheckInRepository {

    /**
     * Check in for a visit.
     * @param visitId The ID of the visit to check in for
     * @param method The method used for check-in
     * @return Result containing the CheckIn record or an error
     */
    suspend fun checkIn(visitId: String, method: CheckInMethod): Result<CheckIn>

    /**
     * Check out from a visit.
     * @param checkInId The ID of the check-in record
     * @param notes Optional notes about the visit
     * @param rating Optional rating (1-5) for the visit experience
     * @return Result containing the updated CheckIn record or an error
     */
    suspend fun checkOut(checkInId: String, notes: String?, rating: Int?): Result<CheckIn>

    /**
     * Generate a QR code for a visit.
     * @param visitId The ID of the visit
     * @return Result containing the QR code data or an error
     */
    suspend fun generateQrCode(visitId: String): Result<QrCodeData>

    /**
     * Validate QR code data and return the associated visit.
     * @param qrData The raw QR code data string
     * @return Result containing the validation result
     */
    suspend fun validateQrCode(qrData: String): Result<QrValidationResult>

    /**
     * Get the active check-in for a visit (if any).
     * @param visitId The ID of the visit
     * @return Flow emitting the active CheckIn or null
     */
    fun getActiveCheckIn(visitId: String): Flow<CheckIn?>

    /**
     * Get a check-in record by ID.
     * @param checkInId The ID of the check-in
     * @return Result containing the CheckIn or an error
     */
    suspend fun getCheckInById(checkInId: String): Result<CheckIn>

    /**
     * Get all check-ins for a visit.
     * @param visitId The ID of the visit
     * @return Flow emitting the list of check-ins
     */
    fun getCheckInsForVisit(visitId: String): Flow<List<CheckIn>>

    /**
     * Get today's expected visitors.
     * @return Flow emitting the list of expected visitors
     */
    fun getTodayExpectedVisitors(): Flow<List<ExpectedVisitor>>

    /**
     * Get expected visitors for a specific date.
     * @param date The date to get visitors for
     * @return Flow emitting the list of expected visitors
     */
    fun getExpectedVisitorsForDate(date: LocalDate): Flow<List<ExpectedVisitor>>

    /**
     * Generate a visitor badge for an active check-in.
     * @param checkInId The ID of the check-in
     * @return Result containing the VisitorBadge or an error
     */
    suspend fun generateVisitorBadge(checkInId: String): Result<VisitorBadge>

    /**
     * Verify a visitor badge QR code.
     * @param badgeQrData The QR code data from the badge
     * @return Result containing the badge verification status
     */
    suspend fun verifyBadge(badgeQrData: String): Result<VisitorBadge>

    /**
     * Get check-in statistics for a date range.
     * @param startDate The start date
     * @param endDate The end date
     * @return Result containing check-in statistics
     */
    suspend fun getCheckInStatistics(startDate: LocalDate, endDate: LocalDate): Result<CheckInStatistics>

    /**
     * Sync check-ins from remote server.
     * @return Result indicating success or failure
     */
    suspend fun syncCheckIns(): Result<Unit>
}

/**
 * Statistics about check-ins.
 */
data class CheckInStatistics(
    val totalCheckIns: Int,
    val qrCodeCheckIns: Int,
    val manualCheckIns: Int,
    val automaticCheckIns: Int,
    val averageVisitDurationMinutes: Int,
    val averageRating: Float?,
    val onTimePercentage: Float,
    val latePercentage: Float,
    val noShowCount: Int
)
