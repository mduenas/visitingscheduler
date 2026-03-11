package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.ApprovalLevel
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import com.markduenas.visischeduler.domain.repository.ViolationType
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import java.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Intelligent engine for evaluating scheduling rules and restrictions.
 */
class EvaluateRulesUseCase(
    private val restrictionRepository: RestrictionRepository,
    private val visitRepository: VisitRepository
) {
    /**
     * Evaluates a proposed visit against all active rules and restrictions.
     * @return Result containing list of violations (empty if all checks pass)
     */
    suspend operator fun invoke(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int = 0
    ): Result<List<RestrictionViolation>> {
        return try {
            val allRestrictions = restrictionRepository.getActiveRestrictions().first()
            val existingVisits = visitRepository.getVisitsForBeneficiary(beneficiaryId).first()
            val visitorVisits = visitRepository.getMyVisits().first() // Assuming current user is the visitor

            val violations = mutableListOf<RestrictionViolation>()

            // 1. Filter applicable restrictions
            val applicableRestrictions = allRestrictions.filter { restriction ->
                isRestrictionApplicable(restriction, visitorId, beneficiaryId, visitDate)
            }

            // 2. Evaluate each restriction
            for (restriction in applicableRestrictions) {
                val violation = evaluateRestriction(
                    restriction,
                    visitorId,
                    beneficiaryId,
                    visitDate,
                    startTime,
                    endTime,
                    additionalVisitorCount,
                    existingVisits,
                    visitorVisits
                )
                if (violation != null) {
                    violations.add(violation)
                }
            }

            Result.success(violations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isRestrictionApplicable(
        restriction: Restriction,
        visitorId: String,
        beneficiaryId: String,
        date: LocalDate
    ): Boolean {
        // Check if restriction is currently active
        if (!restriction.isActive) return false
        if (date < restriction.effectiveFrom) return false
        if (restriction.effectiveUntil != null && date > restriction.effectiveUntil) return false

        // Check scope
        return when (restriction.scope) {
            RestrictionScope.GLOBAL -> true
            RestrictionScope.FACILITY_WIDE -> true // Could add facilityId check if available
            RestrictionScope.BENEFICIARY_SPECIFIC -> restriction.facilityId == null || true // Placeholder
            RestrictionScope.VISITOR_SPECIFIC -> restriction.visitorConstraints?.blockedVisitorIds?.contains(visitorId) == true ||
                                               restriction.visitorConstraints?.allowedVisitorIds?.contains(visitorId) == true
            RestrictionScope.VISITOR_BENEFICIARY_PAIR -> true // Placeholder
        }
    }

    private fun evaluateRestriction(
        restriction: Restriction,
        visitorId: String,
        beneficiaryId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int,
        existingVisits: List<Visit>,
        visitorVisits: List<Visit>
    ): RestrictionViolation? {
        // Time Constraints
        restriction.timeConstraints?.let { tc ->
            // Day of week check
            if (tc.blockedDays?.contains(date.dayOfWeek) == true) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.TIME_VIOLATION,
                    "Visits are blocked on ${date.dayOfWeek}",
                    "Try scheduling on a different day"
                )
            }
            if (tc.allowedDays != null && !tc.allowedDays.contains(date.dayOfWeek)) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.TIME_VIOLATION,
                    "Visits are only allowed on ${tc.allowedDays.joinToString()}",
                    "Try scheduling on one of the allowed days"
                )
            }

            // Time range check
            if (tc.earliestStartTime != null && startTime < tc.earliestStartTime) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.TIME_VIOLATION,
                    "Visit starts too early. Earliest allowed start is ${tc.earliestStartTime}",
                    "Try a later start time"
                )
            }
            if (tc.latestEndTime != null && endTime > tc.latestEndTime) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.TIME_VIOLATION,
                    "Visit ends too late. Latest allowed end is ${tc.latestEndTime}",
                    "Try an earlier end time"
                )
            }

            // Duration check
            val durationMinutes = calculateDurationMinutes(startTime, endTime)
            if (tc.maxDurationMinutes != null && durationMinutes > tc.maxDurationMinutes) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.TIME_VIOLATION,
                    "Visit exceeds maximum duration of ${tc.maxDurationMinutes} minutes",
                    "Reduce the visit duration"
                )
            }

            // Gap check
            if (tc.requiredGapBetweenVisitsHours != null) {
                val lastVisit = existingVisits.filter { it.scheduledDate == date }
                    .maxByOrNull { it.endTime }
                if (lastVisit != null) {
                    val gap = calculateGapHours(lastVisit.endTime, startTime)
                    if (gap < tc.requiredGapBetweenVisitsHours) {
                        return RestrictionViolation(
                            restriction,
                            ViolationType.GAP_REQUIREMENT_VIOLATION,
                            "Required gap of ${tc.requiredGapBetweenVisitsHours} hours between visits not met",
                            "Try scheduling at least ${tc.requiredGapBetweenVisitsHours} hours after the previous visit"
                        )
                    }
                }
            }
        }

        // Visitor Constraints
        restriction.visitorConstraints?.let { vc ->
            if (vc.blockedVisitorIds?.contains(visitorId) == true) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.VISITOR_BLOCKED,
                    "You are currently restricted from scheduling visits",
                    "Contact the coordinator for more information"
                )
            }

            if (vc.allowedVisitorIds != null && !vc.allowedVisitorIds.contains(visitorId)) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.VISITOR_BLOCKED,
                    "Only pre-approved visitors can schedule visits",
                    "Request to be added to the approved visitor list"
                )
            }

            // Visit limits
            if (vc.maxVisitsPerDay != null) {
                val todayCount = visitorVisits.count { it.scheduledDate == date && it.status != VisitStatus.CANCELLED }
                if (todayCount >= vc.maxVisitsPerDay) {
                    return RestrictionViolation(
                        restriction,
                        ViolationType.LIMIT_EXCEEDED,
                        "Maximum daily visit limit of ${vc.maxVisitsPerDay} reached",
                        "Try scheduling on a different day"
                    )
                }
            }

            // Guest limits
            if (!vc.canBringGuests && additionalVisitorCount > 0) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.OTHER,
                    "Additional guests are not allowed for this visit",
                    "Remove additional visitors"
                )
            }
            if (vc.maxAdditionalGuests != null && additionalVisitorCount > vc.maxAdditionalGuests) {
                return RestrictionViolation(
                    restriction,
                    ViolationType.OTHER,
                    "Maximum ${vc.maxAdditionalGuests} additional guests allowed",
                    "Reduce the number of guests"
                )
            }
        }

        // Beneficiary Constraints
        restriction.beneficiaryConstraints?.let { bc ->
            if (bc.maxSimultaneousVisitors != null) {
                val simultaneousVisits = existingVisits.filter { visit ->
                    visit.scheduledDate == date &&
                    visit.status != VisitStatus.CANCELLED &&
                    isOverlapping(visit.startTime, visit.endTime, startTime, endTime)
                }
                val currentVisitorCount = simultaneousVisits.sumOf { 1 + it.additionalVisitors.size }
                if (currentVisitorCount + 1 + additionalVisitorCount > bc.maxSimultaneousVisitors) {
                    return RestrictionViolation(
                        restriction,
                        ViolationType.CAPACITY_EXCEEDED,
                        "Maximum capacity of ${bc.maxSimultaneousVisitors} visitors would be exceeded",
                        "Try a different time slot when fewer people are visiting"
                    )
                }
            }

            if (bc.restPeriodHours != null) {
                val nearVisits = existingVisits.filter { it.scheduledDate == date && it.status != VisitStatus.CANCELLED }
                for (visit in nearVisits) {
                    val gapBefore = calculateGapHours(visit.endTime, startTime)
                    val gapAfter = calculateGapHours(endTime, visit.startTime)
                    
                    if ((gapBefore >= 0 && gapBefore < bc.restPeriodHours) ||
                        (gapAfter >= 0 && gapAfter < bc.restPeriodHours)) {
                        return RestrictionViolation(
                            restriction,
                            ViolationType.GAP_REQUIREMENT_VIOLATION,
                            "Mandatory rest period of ${bc.restPeriodHours} hours required between visits",
                            "Try scheduling with a larger gap from other visits"
                        )
                    }
                }
            }
        }

        return null
    }

    private fun calculateDurationMinutes(startTime: LocalTime, endTime: LocalTime): Int {
        val startMinutes = startTime.hour * 60 + startTime.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        return endMinutes - startMinutes
    }

    private fun calculateGapHours(beforeTime: LocalTime, afterTime: LocalTime): Double {
        val beforeMinutes = beforeTime.hour * 60 + beforeTime.minute
        val afterMinutes = afterTime.hour * 60 + afterTime.minute
        return (afterMinutes - beforeMinutes) / 60.0
    }

    private fun isOverlapping(s1: LocalTime, e1: LocalTime, s2: LocalTime, e2: LocalTime): Boolean {
        return s1 < e2 && s2 < e1
    }
}
