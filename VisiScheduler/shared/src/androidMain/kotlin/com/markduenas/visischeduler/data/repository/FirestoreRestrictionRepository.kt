package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.ApprovalLevel
import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import com.markduenas.visischeduler.domain.repository.ViolationType
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import com.markduenas.visischeduler.firebase.QueryOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Firestore implementation of RestrictionRepository.
 */
class FirestoreRestrictionRepository(
    private val firestore: FirestoreDatabase,
    private val currentUserId: () -> String?
) : RestrictionRepository {

    override fun getActiveRestrictions(): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "isActive",
            true
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
    }

    override fun getRestrictionsByType(type: RestrictionType): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "type",
            type.name
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
    }

    override fun getRestrictionsByScope(scope: RestrictionScope): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "scope",
            scope.name
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
    }

    override suspend fun getRestrictionById(restrictionId: String): Result<Restriction> = runCatching {
        firestore.getById(FirestoreDatabase.COLLECTION_RESTRICTIONS, restrictionId)?.toRestriction()
            ?: throw Exception("Restriction not found")
    }

    override fun getRestrictionsForVisitor(visitorId: String): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "visitorConstraints.blockedVisitorIds",
            visitorId
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
    }

    override fun getRestrictionsForBeneficiary(beneficiaryId: String): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "beneficiaryId",
            beneficiaryId
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
    }

    override fun getRestrictionsForVisitorBeneficiaryPair(
        visitorId: String,
        beneficiaryId: String
    ): Flow<List<Restriction>> {
        return getActiveRestrictions().map { restrictions ->
            restrictions.filter { restriction ->
                restriction.scope == RestrictionScope.VISITOR_BENEFICIARY_PAIR &&
                restriction.visitorConstraints?.blockedVisitorIds?.contains(visitorId) == true
            }
        }
    }

    override fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "facilityId",
            facilityId
        ).map { docs ->
            docs.mapNotNull { it.toRestriction() }
                .filter { it.scope == RestrictionScope.FACILITY_WIDE }
        }
    }

    override suspend fun createRestriction(
        name: String,
        description: String,
        type: RestrictionType,
        scope: RestrictionScope,
        priority: Int,
        effectiveFrom: LocalDate,
        effectiveUntil: LocalDate?,
        timeConstraints: TimeConstraints?,
        visitorConstraints: VisitorConstraints?,
        beneficiaryConstraints: BeneficiaryConstraints?,
        facilityId: String?
    ): Result<Restriction> = runCatching {
        val userId = currentUserId() ?: throw Exception("User not authenticated")

        val data = mapOf(
            "name" to name,
            "description" to description,
            "type" to type.name,
            "scope" to scope.name,
            "priority" to priority,
            "isActive" to true,
            "effectiveFrom" to effectiveFrom.toString(),
            "effectiveUntil" to effectiveUntil?.toString(),
            "timeConstraints" to timeConstraints?.toMap(),
            "visitorConstraints" to visitorConstraints?.toMap(),
            "beneficiaryConstraints" to beneficiaryConstraints?.toMap(),
            "facilityId" to facilityId,
            "createdBy" to userId,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        val id = firestore.createRestriction(data)

        Restriction(
            id = id,
            name = name,
            description = description,
            type = type,
            scope = scope,
            priority = priority,
            isActive = true,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil,
            timeConstraints = timeConstraints,
            visitorConstraints = visitorConstraints,
            beneficiaryConstraints = beneficiaryConstraints,
            facilityId = facilityId,
            createdBy = userId,
            createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
    }

    override suspend fun updateRestriction(restriction: Restriction): Result<Restriction> = runCatching {
        val updates = mapOf(
            "name" to restriction.name,
            "description" to restriction.description,
            "type" to restriction.type.name,
            "scope" to restriction.scope.name,
            "priority" to restriction.priority,
            "isActive" to restriction.isActive,
            "effectiveFrom" to restriction.effectiveFrom.toString(),
            "effectiveUntil" to restriction.effectiveUntil?.toString(),
            "timeConstraints" to restriction.timeConstraints?.toMap(),
            "visitorConstraints" to restriction.visitorConstraints?.toMap(),
            "beneficiaryConstraints" to restriction.beneficiaryConstraints?.toMap(),
            "updatedAt" to Timestamp.now()
        )

        firestore.updateRestriction(restriction.id, updates)
        restriction.copy(updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()))
    }

    override suspend fun deactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        firestore.updateRestriction(restrictionId, mapOf(
            "isActive" to false,
            "updatedAt" to Timestamp.now()
        ))

        firestore.getById(FirestoreDatabase.COLLECTION_RESTRICTIONS, restrictionId)?.toRestriction()
            ?: throw Exception("Restriction not found")
    }

    override suspend fun reactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        firestore.updateRestriction(restrictionId, mapOf(
            "isActive" to true,
            "updatedAt" to Timestamp.now()
        ))

        firestore.getById(FirestoreDatabase.COLLECTION_RESTRICTIONS, restrictionId)?.toRestriction()
            ?: throw Exception("Restriction not found")
    }

    override suspend fun deleteRestriction(restrictionId: String): Result<Unit> = runCatching {
        firestore.deleteRestriction(restrictionId)
    }

    override suspend fun checkVisitRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<Restriction>> = runCatching {
        val allRestrictions = firestore.getRestrictionsForBeneficiary(beneficiaryId)
            .mapNotNull { it.toRestriction() }
            .filter { it.isActive }

        allRestrictions.filter { restriction ->
            checkRestrictionViolation(restriction, visitorId, visitDate, startTime, endTime, additionalVisitorCount)
        }
    }

    override suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<RestrictionViolation>> = runCatching {
        val violations = checkVisitRestrictions(
            visitorId, beneficiaryId, visitDate, startTime, endTime, additionalVisitorCount
        ).getOrThrow()

        violations.map { restriction ->
            RestrictionViolation(
                restriction = restriction,
                violationType = determineViolationType(restriction),
                message = generateViolationMessage(restriction),
                suggestedResolution = generateSuggestedResolution(restriction)
            )
        }
    }

    override suspend fun syncRestrictions(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    // ==================== Helper Functions ====================

    private fun checkRestrictionViolation(
        restriction: Restriction,
        visitorId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Boolean {
        // Check if date is within effective range
        if (visitDate < restriction.effectiveFrom) return false
        if (restriction.effectiveUntil != null && visitDate > restriction.effectiveUntil) return false

        // Check time constraints
        restriction.timeConstraints?.let { tc ->
            tc.blockedDays?.let { blocked ->
                if (visitDate.dayOfWeek in blocked) return true
            }
            tc.earliestStartTime?.let { earliest ->
                if (startTime < earliest) return true
            }
            tc.latestEndTime?.let { latest ->
                if (endTime > latest) return true
            }
        }

        // Check visitor constraints
        restriction.visitorConstraints?.let { vc ->
            vc.blockedVisitorIds?.let { blocked ->
                if (visitorId in blocked) return true
            }
            vc.maxAdditionalGuests?.let { max ->
                if (additionalVisitorCount > max) return true
            }
        }

        return false
    }

    private fun determineViolationType(restriction: Restriction): ViolationType {
        return when (restriction.type) {
            RestrictionType.TIME_BASED -> ViolationType.TIME_VIOLATION
            RestrictionType.VISITOR_BASED -> ViolationType.VISITOR_BLOCKED
            RestrictionType.CAPACITY_BASED -> ViolationType.CAPACITY_EXCEEDED
            else -> ViolationType.OTHER
        }
    }

    private fun generateViolationMessage(restriction: Restriction): String {
        return "Visit violates restriction: ${restriction.name}. ${restriction.description}"
    }

    private fun generateSuggestedResolution(restriction: Restriction): String? {
        return when (restriction.type) {
            RestrictionType.TIME_BASED -> "Try selecting a different time slot"
            RestrictionType.VISITOR_BASED -> "Contact the coordinator for assistance"
            RestrictionType.CAPACITY_BASED -> "Reduce the number of visitors"
            else -> null
        }
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toRestriction(): Restriction? {
        return try {
            Restriction(
                id = id,
                name = getString("name") ?: return null,
                description = getString("description") ?: "",
                type = RestrictionType.valueOf(getString("type") ?: "TIME_BASED"),
                scope = RestrictionScope.valueOf(getString("scope") ?: "FACILITY_WIDE"),
                priority = getLong("priority")?.toInt() ?: 0,
                isActive = getBoolean("isActive") ?: true,
                effectiveFrom = LocalDate.parse(getString("effectiveFrom") ?: return null),
                effectiveUntil = getString("effectiveUntil")?.let { LocalDate.parse(it) },
                timeConstraints = (get("timeConstraints") as? Map<*, *>)?.toTimeConstraints(),
                visitorConstraints = (get("visitorConstraints") as? Map<*, *>)?.toVisitorConstraints(),
                beneficiaryConstraints = (get("beneficiaryConstraints") as? Map<*, *>)?.toBeneficiaryConstraints(),
                facilityId = getString("facilityId"),
                createdBy = getString("createdBy") ?: "",
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

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.toTimeConstraints(): TimeConstraints {
        return TimeConstraints(
            allowedDays = (this["allowedDays"] as? List<String>)?.map { DayOfWeek.valueOf(it) },
            blockedDays = (this["blockedDays"] as? List<String>)?.map { DayOfWeek.valueOf(it) },
            earliestStartTime = (this["earliestStartTime"] as? String)?.let { LocalTime.parse(it) },
            latestEndTime = (this["latestEndTime"] as? String)?.let { LocalTime.parse(it) },
            maxDurationMinutes = (this["maxDurationMinutes"] as? Number)?.toInt(),
            minAdvanceBookingHours = (this["minAdvanceBookingHours"] as? Number)?.toInt(),
            maxAdvanceBookingDays = (this["maxAdvanceBookingDays"] as? Number)?.toInt(),
            requiredGapBetweenVisitsHours = (this["requiredGapBetweenVisitsHours"] as? Number)?.toInt()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.toVisitorConstraints(): VisitorConstraints {
        return VisitorConstraints(
            blockedVisitorIds = this["blockedVisitorIds"] as? List<String>,
            allowedVisitorIds = this["allowedVisitorIds"] as? List<String>,
            maxVisitsPerDay = (this["maxVisitsPerDay"] as? Number)?.toInt(),
            maxVisitsPerWeek = (this["maxVisitsPerWeek"] as? Number)?.toInt(),
            maxVisitsPerMonth = (this["maxVisitsPerMonth"] as? Number)?.toInt(),
            requiredApprovalLevel = (this["requiredApprovalLevel"] as? String)?.let { ApprovalLevel.valueOf(it) },
            requiresEscort = this["requiresEscort"] as? Boolean ?: false,
            canBringGuests = this["canBringGuests"] as? Boolean ?: true,
            maxAdditionalGuests = (this["maxAdditionalGuests"] as? Number)?.toInt()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.toBeneficiaryConstraints(): BeneficiaryConstraints {
        return BeneficiaryConstraints(
            maxSimultaneousVisitors = (this["maxSimultaneousVisitors"] as? Number)?.toInt(),
            maxVisitsPerDay = (this["maxVisitsPerDay"] as? Number)?.toInt(),
            maxVisitsPerWeek = (this["maxVisitsPerWeek"] as? Number)?.toInt(),
            restPeriodHours = (this["restPeriodHours"] as? Number)?.toInt(),
            allowedVisitTypes = (this["allowedVisitTypes"] as? List<String>)?.map { VisitType.valueOf(it) },
            requiresMedicalClearance = this["requiresMedicalClearance"] as? Boolean ?: false,
            specialInstructions = this["specialInstructions"] as? String
        )
    }

    private fun TimeConstraints.toMap(): Map<String, Any?> = mapOf(
        "allowedDays" to allowedDays?.map { it.name },
        "blockedDays" to blockedDays?.map { it.name },
        "earliestStartTime" to earliestStartTime?.toString(),
        "latestEndTime" to latestEndTime?.toString(),
        "maxDurationMinutes" to maxDurationMinutes,
        "minAdvanceBookingHours" to minAdvanceBookingHours,
        "maxAdvanceBookingDays" to maxAdvanceBookingDays,
        "requiredGapBetweenVisitsHours" to requiredGapBetweenVisitsHours
    )

    private fun VisitorConstraints.toMap(): Map<String, Any?> = mapOf(
        "blockedVisitorIds" to blockedVisitorIds,
        "allowedVisitorIds" to allowedVisitorIds,
        "maxVisitsPerDay" to maxVisitsPerDay,
        "maxVisitsPerWeek" to maxVisitsPerWeek,
        "maxVisitsPerMonth" to maxVisitsPerMonth,
        "requiredApprovalLevel" to requiredApprovalLevel?.name,
        "requiresEscort" to requiresEscort,
        "canBringGuests" to canBringGuests,
        "maxAdditionalGuests" to maxAdditionalGuests
    )

    private fun BeneficiaryConstraints.toMap(): Map<String, Any?> = mapOf(
        "maxSimultaneousVisitors" to maxSimultaneousVisitors,
        "maxVisitsPerDay" to maxVisitsPerDay,
        "maxVisitsPerWeek" to maxVisitsPerWeek,
        "restPeriodHours" to restPeriodHours,
        "allowedVisitTypes" to allowedVisitTypes?.map { it.name },
        "requiresMedicalClearance" to requiresMedicalClearance,
        "specialInstructions" to specialInstructions
    )
}
