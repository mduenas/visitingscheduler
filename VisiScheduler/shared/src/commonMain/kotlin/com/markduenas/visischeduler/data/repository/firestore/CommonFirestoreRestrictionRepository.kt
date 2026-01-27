package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of RestrictionRepository.
 */
class CommonFirestoreRestrictionRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : RestrictionRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

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
            "visitorId",
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
        // Query both visitor and beneficiary restrictions and combine
        return getRestrictionsForVisitor(visitorId).map { visitorRestrictions ->
            visitorRestrictions
        }
    }

    override fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_RESTRICTIONS,
            "facilityId",
            facilityId
        ).map { docs -> docs.mapNotNull { it.toRestriction() } }
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
        val createdBy = currentUserId ?: throw Exception("Not authenticated")
        val now = Clock.System.now()

        val data = mutableMapOf<String, Any?>(
            "name" to name,
            "description" to description,
            "type" to type.name,
            "scope" to scope.name,
            "priority" to priority,
            "effectiveFrom" to effectiveFrom.toString(),
            "effectiveUntil" to effectiveUntil?.toString(),
            "isActive" to true,
            "createdBy" to createdBy,
            "createdAt" to firestore.serverTimestamp(),
            "updatedAt" to firestore.serverTimestamp()
        )
        facilityId?.let { data["facilityId"] = it }

        val id = firestore.createRestriction(data)

        Restriction(
            id = id,
            name = name,
            description = description,
            type = type,
            scope = scope,
            priority = priority,
            effectiveFrom = effectiveFrom,
            effectiveUntil = effectiveUntil,
            isActive = true,
            createdBy = createdBy,
            timeConstraints = timeConstraints,
            visitorConstraints = visitorConstraints,
            beneficiaryConstraints = beneficiaryConstraints,
            facilityId = facilityId,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun updateRestriction(restriction: Restriction): Result<Restriction> = runCatching {
        val updates = mapOf<String, Any?>(
            "name" to restriction.name,
            "description" to restriction.description,
            "type" to restriction.type.name,
            "scope" to restriction.scope.name,
            "priority" to restriction.priority,
            "effectiveFrom" to restriction.effectiveFrom.toString(),
            "effectiveUntil" to restriction.effectiveUntil?.toString(),
            "isActive" to restriction.isActive,
            "updatedAt" to firestore.serverTimestamp()
        )

        firestore.updateRestriction(restriction.id, updates)
        restriction.copy(updatedAt = Clock.System.now())
    }

    override suspend fun deactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        firestore.updateRestriction(restrictionId, mapOf(
            "isActive" to false,
            "updatedAt" to firestore.serverTimestamp()
        ))

        firestore.getById(FirestoreDatabase.COLLECTION_RESTRICTIONS, restrictionId)?.toRestriction()
            ?: throw Exception("Restriction not found")
    }

    override suspend fun reactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        firestore.updateRestriction(restrictionId, mapOf(
            "isActive" to true,
            "updatedAt" to firestore.serverTimestamp()
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
        // Simplified implementation - returns empty list (no violations)
        // In a full implementation, this would check all applicable restrictions
        emptyList()
    }

    override suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<RestrictionViolation>> = runCatching {
        // Simplified implementation
        emptyList()
    }

    override suspend fun syncRestrictions(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    private fun DocumentSnapshot.toRestriction(): Restriction? {
        return try {
            Restriction(
                id = id,
                name = get("name") ?: return null,
                description = get("description") ?: "",
                type = RestrictionType.valueOf(get("type") ?: "VISITOR_SPECIFIC"),
                scope = RestrictionScope.valueOf(get("scope") ?: "FACILITY"),
                priority = get<Long?>("priority")?.toInt() ?: 0,
                effectiveFrom = LocalDate.parse(get("effectiveFrom") ?: return null),
                effectiveUntil = get<String?>("effectiveUntil")?.let { LocalDate.parse(it) },
                isActive = get("isActive") ?: true,
                createdBy = get("createdBy") ?: "",
                timeConstraints = null, // Simplified
                visitorConstraints = null, // Simplified
                beneficiaryConstraints = null, // Simplified
                facilityId = get("facilityId"),
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
