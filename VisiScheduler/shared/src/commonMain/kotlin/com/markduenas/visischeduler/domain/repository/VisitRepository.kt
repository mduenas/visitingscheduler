package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Repository interface for visit operations.
 */
interface VisitRepository {
    /**
     * Get all visits for the current user.
     */
    fun getMyVisits(): Flow<List<Visit>>

    /**
     * Get visits by status for the current user.
     */
    fun getMyVisitsByStatus(status: VisitStatus): Flow<List<Visit>>

    /**
     * Get upcoming visits for the current user.
     */
    fun getUpcomingVisits(): Flow<List<Visit>>

    /**
     * Get past visits for the current user.
     */
    fun getPastVisits(): Flow<List<Visit>>

    /**
     * Get a specific visit by ID.
     */
    suspend fun getVisitById(visitId: String): Result<Visit>

    /**
     * Get visits for a specific beneficiary.
     */
    fun getVisitsForBeneficiary(beneficiaryId: String): Flow<List<Visit>>

    /**
     * Get visits pending approval (for coordinators).
     */
    fun getPendingApprovalVisits(): Flow<List<Visit>>

    /**
     * Get visits for a specific date range.
     */
    fun getVisitsInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Visit>>

    /**
     * Schedule a new visit.
     */
    suspend fun scheduleVisit(
        beneficiaryId: String,
        scheduledDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        visitType: VisitType = VisitType.IN_PERSON,
        purpose: String? = null,
        notes: String? = null,
        additionalVisitors: List<AdditionalVisitor> = emptyList(),
        videoCallLink: String? = null,
        videoCallPlatform: String? = null
    ): Result<Visit>

    /**
     * Update an existing visit.
     */
    suspend fun updateVisit(visit: Visit): Result<Visit>

    /**
     * Cancel a visit.
     */
    suspend fun cancelVisit(visitId: String, reason: String): Result<Visit>

    /**
     * Approve a visit (coordinator action).
     */
    suspend fun approveVisit(visitId: String, notes: String? = null): Result<Visit>

    /**
     * Deny a visit (coordinator action).
     */
    suspend fun denyVisit(visitId: String, reason: String): Result<Visit>

    /**
     * Check in for a visit.
     */
    suspend fun checkIn(visitId: String): Result<Visit>

    /**
     * Check out from a visit.
     */
    suspend fun checkOut(visitId: String): Result<Visit>

    /**
     * Mark a visit as no-show.
     */
    suspend fun markAsNoShow(visitId: String): Result<Visit>

    /**
     * Reschedule a visit.
     */
    suspend fun rescheduleVisit(
        visitId: String,
        newDate: LocalDate,
        newStartTime: LocalTime,
        newEndTime: LocalTime
    ): Result<Visit>

    /**
     * Get visit statistics for the current user.
     */
    suspend fun getVisitStatistics(): Result<VisitStatistics>

    /**
     * Sync visits from remote server.
     */
    suspend fun syncVisits(): Result<Unit>
}

/**
 * Statistics about visits.
 */
data class VisitStatistics(
    val totalVisits: Int,
    val completedVisits: Int,
    val cancelledVisits: Int,
    val noShowVisits: Int,
    val upcomingVisits: Int,
    val pendingVisits: Int
)
