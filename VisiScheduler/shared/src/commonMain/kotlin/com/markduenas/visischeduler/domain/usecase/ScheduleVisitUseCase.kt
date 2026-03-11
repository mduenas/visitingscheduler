package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for scheduling visits.
 */
class ScheduleVisitUseCase(
    private val visitRepository: VisitRepository,
    private val evaluateRulesUseCase: EvaluateRulesUseCase
) {
    /**
     * Schedule a new visit.
     * @param request The visit scheduling request
     * @return Result containing the scheduled Visit or an error
     */
    suspend operator fun invoke(request: ScheduleVisitRequest): Result<Visit> {
        // Validate the request
        val validationResult = validateRequest(request)
        if (validationResult != null) {
            return Result.failure(validationResult)
        }

        // Check for restriction violations using the Intelligent Rule Engine
        val evaluationResult = evaluateRulesUseCase(
            visitorId = request.visitorId,
            beneficiaryId = request.beneficiaryId,
            visitDate = request.scheduledDate,
            startTime = request.startTime,
            endTime = request.endTime,
            additionalVisitorCount = request.additionalVisitors.size
        )

        if (evaluationResult.isFailure) {
            return Result.failure(
                ScheduleVisitException.RestrictionCheckFailed(
                    "Failed to evaluate scheduling rules: ${evaluationResult.exceptionOrNull()?.message}"
                )
            )
        }

        val violations = evaluationResult.getOrNull() ?: emptyList()
        if (violations.isNotEmpty()) {
            return Result.failure(
                ScheduleVisitException.DetailedRestrictionViolation(
                    "Visit violates restrictions",
                    violations
                )
            )
        }

        // Schedule the visit
        return visitRepository.scheduleVisit(
            beneficiaryId = request.beneficiaryId,
            scheduledDate = request.scheduledDate,
            startTime = request.startTime,
            endTime = request.endTime,
            visitType = request.visitType,
            purpose = request.purpose,
            notes = request.notes,
            additionalVisitors = request.additionalVisitors,
            videoCallLink = request.videoCallLink,
            videoCallPlatform = request.videoCallPlatform
        )
    }

    private fun validateRequest(request: ScheduleVisitRequest): ScheduleVisitException? {
        // Check if date is in the future
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (request.scheduledDate < today) {
            return ScheduleVisitException.InvalidDate("Cannot schedule visits in the past")
        }

        // Check if end time is after start time
        if (request.endTime <= request.startTime) {
            return ScheduleVisitException.InvalidTime("End time must be after start time")
        }

        // Check minimum duration (30 minutes)
        val durationMinutes = calculateDurationMinutes(request.startTime, request.endTime)
        if (durationMinutes < 30) {
            return ScheduleVisitException.InvalidDuration("Visit must be at least 30 minutes")
        }

        // Check maximum duration (4 hours)
        if (durationMinutes > 240) {
            return ScheduleVisitException.InvalidDuration("Visit cannot exceed 4 hours")
        }

        // Check additional visitors limit
        if (request.additionalVisitors.size > 5) {
            return ScheduleVisitException.TooManyVisitors("Maximum 5 additional visitors allowed")
        }

        return null
    }

    private fun calculateDurationMinutes(startTime: LocalTime, endTime: LocalTime): Int {
        val startMinutes = startTime.hour * 60 + startTime.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        return endMinutes - startMinutes
    }
}

/**
 * Request data for scheduling a visit.
 */
data class ScheduleVisitRequest(
    val visitorId: String,
    val beneficiaryId: String,
    val scheduledDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val visitType: VisitType = VisitType.IN_PERSON,
    val purpose: String? = null,
    val notes: String? = null,
    val additionalVisitors: List<AdditionalVisitor> = emptyList(),
    val videoCallLink: String? = null,
    val videoCallPlatform: String? = null
)

/**
 * Exceptions that can occur during visit scheduling.
 */
sealed class ScheduleVisitException(message: String) : Exception(message) {
    class InvalidDate(message: String) : ScheduleVisitException(message)
    class InvalidTime(message: String) : ScheduleVisitException(message)
    class InvalidDuration(message: String) : ScheduleVisitException(message)
    class TooManyVisitors(message: String) : ScheduleVisitException(message)
    class SlotNotAvailable(message: String) : ScheduleVisitException(message)
    class RestrictionViolation(
        message: String,
        val violations: List<Restriction>
    ) : ScheduleVisitException(message)
    class DetailedRestrictionViolation(
        message: String,
        val violations: List<com.markduenas.visischeduler.domain.repository.RestrictionViolation>
    ) : ScheduleVisitException(message)
    class RestrictionCheckFailed(message: String) : ScheduleVisitException(message)
    class BeneficiaryNotFound(message: String) : ScheduleVisitException(message)
    class NotAuthorized(message: String) : ScheduleVisitException(message)
    class NetworkError(message: String) : ScheduleVisitException(message)
}
