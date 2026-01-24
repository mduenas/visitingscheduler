package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository interface for restriction management.
 */
interface RestrictionRepository {
    /**
     * Get all active restrictions.
     */
    fun getActiveRestrictions(): Flow<List<Restriction>>

    /**
     * Get restrictions by type.
     */
    fun getRestrictionsByType(type: RestrictionType): Flow<List<Restriction>>

    /**
     * Get restrictions by scope.
     */
    fun getRestrictionsByScope(scope: RestrictionScope): Flow<List<Restriction>>

    /**
     * Get a specific restriction by ID.
     */
    suspend fun getRestrictionById(restrictionId: String): Result<Restriction>

    /**
     * Get restrictions applicable to a specific visitor.
     */
    fun getRestrictionsForVisitor(visitorId: String): Flow<List<Restriction>>

    /**
     * Get restrictions applicable to a specific beneficiary.
     */
    fun getRestrictionsForBeneficiary(beneficiaryId: String): Flow<List<Restriction>>

    /**
     * Get restrictions for a visitor-beneficiary pair.
     */
    fun getRestrictionsForVisitorBeneficiaryPair(
        visitorId: String,
        beneficiaryId: String
    ): Flow<List<Restriction>>

    /**
     * Get facility-wide restrictions.
     */
    fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>>

    /**
     * Create a new restriction.
     */
    suspend fun createRestriction(
        name: String,
        description: String,
        type: RestrictionType,
        scope: RestrictionScope,
        priority: Int = 0,
        effectiveFrom: LocalDate,
        effectiveUntil: LocalDate? = null,
        timeConstraints: TimeConstraints? = null,
        visitorConstraints: VisitorConstraints? = null,
        beneficiaryConstraints: BeneficiaryConstraints? = null,
        facilityId: String? = null
    ): Result<Restriction>

    /**
     * Update an existing restriction.
     */
    suspend fun updateRestriction(restriction: Restriction): Result<Restriction>

    /**
     * Deactivate a restriction.
     */
    suspend fun deactivateRestriction(restrictionId: String): Result<Restriction>

    /**
     * Reactivate a restriction.
     */
    suspend fun reactivateRestriction(restrictionId: String): Result<Restriction>

    /**
     * Delete a restriction.
     */
    suspend fun deleteRestriction(restrictionId: String): Result<Unit>

    /**
     * Check if a visit would violate any restrictions.
     * @return List of violated restrictions, empty if no violations
     */
    suspend fun checkVisitRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: kotlinx.datetime.LocalTime,
        endTime: kotlinx.datetime.LocalTime,
        additionalVisitorCount: Int = 0
    ): Result<List<Restriction>>

    /**
     * Get restriction violations for a proposed visit with explanations.
     */
    suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: kotlinx.datetime.LocalTime,
        endTime: kotlinx.datetime.LocalTime,
        additionalVisitorCount: Int = 0
    ): Result<List<RestrictionViolation>>

    /**
     * Sync restrictions from remote server.
     */
    suspend fun syncRestrictions(): Result<Unit>
}

/**
 * Represents a restriction violation with explanation.
 */
data class RestrictionViolation(
    val restriction: Restriction,
    val violationType: ViolationType,
    val message: String,
    val suggestedResolution: String? = null
)

/**
 * Type of restriction violation.
 */
enum class ViolationType {
    /** Time constraint violated */
    TIME_VIOLATION,
    /** Visitor not allowed */
    VISITOR_BLOCKED,
    /** Visit limit exceeded */
    LIMIT_EXCEEDED,
    /** Capacity exceeded */
    CAPACITY_EXCEEDED,
    /** Advance booking requirement not met */
    BOOKING_WINDOW_VIOLATION,
    /** Required gap between visits not met */
    GAP_REQUIREMENT_VIOLATION,
    /** Beneficiary constraint violated */
    BENEFICIARY_CONSTRAINT_VIOLATION,
    /** Other violation */
    OTHER
}
