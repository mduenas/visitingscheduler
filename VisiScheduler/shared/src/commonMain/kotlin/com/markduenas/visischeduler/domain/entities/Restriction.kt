package com.markduenas.visischeduler.domain.entities

import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a restriction rule in the VisiScheduler system.
 * Restrictions can be time-based, visitor-based, or beneficiary-based.
 */
@Serializable
data class Restriction(
    val id: String,
    val name: String,
    val description: String,
    val type: RestrictionType,
    val scope: RestrictionScope,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val effectiveFrom: LocalDate,
    val effectiveUntil: LocalDate? = null,
    val timeConstraints: TimeConstraints? = null,
    val visitorConstraints: VisitorConstraints? = null,
    val beneficiaryConstraints: BeneficiaryConstraints? = null,
    val facilityId: String? = null,
    val createdBy: String,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)

/**
 * Type of restriction.
 */
@Serializable
enum class RestrictionType {
    /** Time-based restrictions (hours, days) */
    TIME_BASED,
    /** Visitor-specific restrictions */
    VISITOR_BASED,
    /** Beneficiary-specific restrictions */
    BENEFICIARY_BASED,
    /** Capacity-based restrictions */
    CAPACITY_BASED,
    /** Relationship-based restrictions */
    RELATIONSHIP_BASED,
    /** Combined/complex restriction */
    COMBINED
}

/**
 * Scope of the restriction application.
 */
@Serializable
enum class RestrictionScope {
    /** Applies to entire facility */
    FACILITY_WIDE,
    /** Applies to specific beneficiary */
    BENEFICIARY_SPECIFIC,
    /** Applies to specific visitor */
    VISITOR_SPECIFIC,
    /** Applies to visitor-beneficiary pair */
    VISITOR_BENEFICIARY_PAIR,
    /** Applies globally across all facilities */
    GLOBAL
}

/**
 * Time-based constraints for a restriction.
 */
@Serializable
data class TimeConstraints(
    /** Days of week when restriction applies */
    val allowedDays: List<DayOfWeek>? = null,
    /** Blocked days of week */
    val blockedDays: List<DayOfWeek>? = null,
    /** Earliest allowed start time */
    val earliestStartTime: LocalTime? = null,
    /** Latest allowed end time */
    val latestEndTime: LocalTime? = null,
    /** Maximum visit duration in minutes */
    val maxDurationMinutes: Int? = null,
    /** Minimum advance booking hours */
    val minAdvanceBookingHours: Int? = null,
    /** Maximum advance booking days */
    val maxAdvanceBookingDays: Int? = null,
    /** Required gap between visits in hours */
    val requiredGapBetweenVisitsHours: Int? = null
)

/**
 * Visitor-based constraints for a restriction.
 */
@Serializable
data class VisitorConstraints(
    /** List of blocked visitor IDs */
    val blockedVisitorIds: List<String>? = null,
    /** List of allowed visitor IDs (whitelist) */
    val allowedVisitorIds: List<String>? = null,
    /** Maximum visits per day for this visitor */
    val maxVisitsPerDay: Int? = null,
    /** Maximum visits per week for this visitor */
    val maxVisitsPerWeek: Int? = null,
    /** Maximum visits per month for this visitor */
    val maxVisitsPerMonth: Int? = null,
    /** Required approval level for visits */
    val requiredApprovalLevel: ApprovalLevel? = null,
    /** Whether visitor requires escort */
    val requiresEscort: Boolean = false,
    /** Whether visitor can bring additional guests */
    val canBringGuests: Boolean = true,
    /** Maximum additional guests allowed */
    val maxAdditionalGuests: Int? = null
)

/**
 * Beneficiary-based constraints for a restriction.
 */
@Serializable
data class BeneficiaryConstraints(
    /** Maximum visitors at one time */
    val maxSimultaneousVisitors: Int? = null,
    /** Maximum visits per day */
    val maxVisitsPerDay: Int? = null,
    /** Maximum visits per week */
    val maxVisitsPerWeek: Int? = null,
    /** Required rest period between visits in hours */
    val restPeriodHours: Int? = null,
    /** Allowed visit types */
    val allowedVisitTypes: List<VisitType>? = null,
    /** Whether medical clearance is required */
    val requiresMedicalClearance: Boolean = false,
    /** Special instructions for visitors */
    val specialInstructions: String? = null
)

/**
 * Level of approval required for a visit.
 */
@Serializable
enum class ApprovalLevel {
    /** No approval required (auto-approve) */
    NONE,
    /** Any coordinator can approve */
    ANY_COORDINATOR,
    /** Primary coordinator must approve */
    PRIMARY_COORDINATOR,
    /** Admin must approve */
    ADMIN_ONLY,
    /** Multiple approvers required */
    MULTI_APPROVAL
}
