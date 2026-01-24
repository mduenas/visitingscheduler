package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.RestrictionDto
import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import com.markduenas.visischeduler.domain.repository.ViolationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of RestrictionRepository.
 */
class RestrictionRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : RestrictionRepository {

    override fun getActiveRestrictions(): Flow<List<Restriction>> = flow {
        val cached = database.visiSchedulerQueries
            .selectActiveRestrictions()
            .executeAsList()
            .map { mapEntityToRestriction(it) }
        emit(cached)

        try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            restrictions.forEach { cacheRestriction(it) }
            emit(restrictions.filter { it.isActive })
        } catch (e: Exception) {
            // Keep cached data
        }
    }

    override fun getRestrictionsByType(type: RestrictionType): Flow<List<Restriction>> = flow {
        val cached = database.visiSchedulerQueries
            .selectRestrictionsByType(type.name)
            .executeAsList()
            .map { mapEntityToRestriction(it) }
        emit(cached)
    }

    override fun getRestrictionsByScope(scope: RestrictionScope): Flow<List<Restriction>> = flow {
        val cached = database.visiSchedulerQueries
            .selectRestrictionsByScope(scope.name)
            .executeAsList()
            .map { mapEntityToRestriction(it) }
        emit(cached)
    }

    override suspend fun getRestrictionById(restrictionId: String): Result<Restriction> {
        return try {
            val restriction = api.getRestrictionById(restrictionId).toDomain()
            cacheRestriction(restriction)
            Result.success(restriction)
        } catch (e: Exception) {
            val cached = database.visiSchedulerQueries
                .selectRestrictionById(restrictionId)
                .executeAsOneOrNull()
            if (cached != null) {
                Result.success(mapEntityToRestriction(cached))
            } else {
                Result.failure(e)
            }
        }
    }

    override fun getRestrictionsForVisitor(visitorId: String): Flow<List<Restriction>> = flow {
        // Get restrictions that apply to this visitor
        val allRestrictions = database.visiSchedulerQueries
            .selectActiveRestrictions()
            .executeAsList()
            .map { mapEntityToRestriction(it) }

        val applicable = allRestrictions.filter { restriction ->
            restriction.scope == RestrictionScope.VISITOR_SPECIFIC ||
            restriction.scope == RestrictionScope.GLOBAL ||
            restriction.visitorConstraints?.blockedVisitorIds?.contains(visitorId) == true ||
            restriction.visitorConstraints?.allowedVisitorIds?.contains(visitorId) == true
        }
        emit(applicable)
    }

    override fun getRestrictionsForBeneficiary(beneficiaryId: String): Flow<List<Restriction>> = flow {
        val allRestrictions = database.visiSchedulerQueries
            .selectActiveRestrictions()
            .executeAsList()
            .map { mapEntityToRestriction(it) }

        val applicable = allRestrictions.filter { restriction ->
            restriction.scope == RestrictionScope.BENEFICIARY_SPECIFIC ||
            restriction.scope == RestrictionScope.GLOBAL
        }
        emit(applicable)
    }

    override fun getRestrictionsForVisitorBeneficiaryPair(
        visitorId: String,
        beneficiaryId: String
    ): Flow<List<Restriction>> = flow {
        val allRestrictions = database.visiSchedulerQueries
            .selectActiveRestrictions()
            .executeAsList()
            .map { mapEntityToRestriction(it) }

        val applicable = allRestrictions.filter { restriction ->
            restriction.scope == RestrictionScope.VISITOR_BENEFICIARY_PAIR ||
            restriction.scope == RestrictionScope.GLOBAL
        }
        emit(applicable)
    }

    override fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>> = flow {
        val cached = database.visiSchedulerQueries
            .selectFacilityRestrictions(facilityId)
            .executeAsList()
            .map { mapEntityToRestriction(it) }
        emit(cached)
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
    ): Result<Restriction> {
        return try {
            val now = Clock.System.now()
            val dto = RestrictionDto(
                id = "", // Server will generate
                name = name,
                description = description,
                type = type.name,
                scope = scope.name,
                priority = priority,
                isActive = true,
                effectiveFrom = effectiveFrom.toString(),
                effectiveUntil = effectiveUntil?.toString(),
                timeConstraints = timeConstraints?.let {
                    com.markduenas.visischeduler.data.remote.dto.TimeConstraintsDto.fromDomain(it)
                },
                visitorConstraints = visitorConstraints?.let {
                    com.markduenas.visischeduler.data.remote.dto.VisitorConstraintsDto.fromDomain(it)
                },
                beneficiaryConstraints = beneficiaryConstraints?.let {
                    com.markduenas.visischeduler.data.remote.dto.BeneficiaryConstraintsDto.fromDomain(it)
                },
                facilityId = facilityId,
                createdBy = "", // Server will set
                createdAt = now.toString(),
                updatedAt = now.toString()
            )
            val restriction = api.createRestriction(dto).toDomain()
            cacheRestriction(restriction)
            Result.success(restriction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRestriction(restriction: Restriction): Result<Restriction> {
        return try {
            val dto = RestrictionDto.fromDomain(restriction)
            val updated = api.updateRestriction(restriction.id, dto).toDomain()
            cacheRestriction(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deactivateRestriction(restrictionId: String): Result<Restriction> {
        return try {
            val current = getRestrictionById(restrictionId).getOrThrow()
            val updated = current.copy(isActive = false)
            updateRestriction(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reactivateRestriction(restrictionId: String): Result<Restriction> {
        return try {
            val current = getRestrictionById(restrictionId).getOrThrow()
            val updated = current.copy(isActive = true)
            updateRestriction(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRestriction(restrictionId: String): Result<Unit> {
        return try {
            api.deleteRestriction(restrictionId)
            database.visiSchedulerQueries.deleteRestriction(restrictionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkVisitRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<Restriction>> {
        return try {
            val violations = api.checkRestrictions(
                visitorId = visitorId,
                beneficiaryId = beneficiaryId,
                visitDate = visitDate.toString(),
                startTime = startTime.toString(),
                endTime = endTime.toString(),
                additionalVisitorCount = additionalVisitorCount
            ).map { it.toDomain() }
            Result.success(violations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<RestrictionViolation>> {
        return try {
            val violations = checkVisitRestrictions(
                visitorId, beneficiaryId, visitDate, startTime, endTime, additionalVisitorCount
            ).getOrThrow()

            val explanations = violations.map { restriction ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = determineViolationType(restriction),
                    message = generateViolationMessage(restriction),
                    suggestedResolution = generateResolutionSuggestion(restriction)
                )
            }
            Result.success(explanations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncRestrictions(): Result<Unit> {
        return try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            database.visiSchedulerQueries.deleteAllRestrictions()
            restrictions.forEach { cacheRestriction(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun determineViolationType(restriction: Restriction): ViolationType {
        return when (restriction.type) {
            RestrictionType.TIME_BASED -> ViolationType.TIME_VIOLATION
            RestrictionType.VISITOR_BASED -> ViolationType.VISITOR_BLOCKED
            RestrictionType.CAPACITY_BASED -> ViolationType.CAPACITY_EXCEEDED
            RestrictionType.BENEFICIARY_BASED -> ViolationType.BENEFICIARY_CONSTRAINT_VIOLATION
            else -> ViolationType.OTHER
        }
    }

    private fun generateViolationMessage(restriction: Restriction): String {
        return "Visit violates restriction: ${restriction.name}. ${restriction.description}"
    }

    private fun generateResolutionSuggestion(restriction: Restriction): String? {
        return when (restriction.type) {
            RestrictionType.TIME_BASED -> "Try scheduling at a different time"
            RestrictionType.CAPACITY_BASED -> "Try a time slot with more availability"
            else -> null
        }
    }

    private fun cacheRestriction(restriction: Restriction) {
        database.visiSchedulerQueries.insertRestriction(
            id = restriction.id,
            name = restriction.name,
            description = restriction.description,
            type = restriction.type.name,
            scope = restriction.scope.name,
            priority = restriction.priority.toLong(),
            isActive = if (restriction.isActive) 1L else 0L,
            effectiveFrom = restriction.effectiveFrom.toString(),
            effectiveUntil = restriction.effectiveUntil?.toString(),
            timeConstraints = restriction.timeConstraints?.let { json.encodeToString(it) },
            visitorConstraints = restriction.visitorConstraints?.let { json.encodeToString(it) },
            beneficiaryConstraints = restriction.beneficiaryConstraints?.let { json.encodeToString(it) },
            facilityId = restriction.facilityId,
            createdBy = restriction.createdBy,
            createdAt = restriction.createdAt.toString(),
            updatedAt = restriction.updatedAt.toString()
        )
    }

    private fun mapEntityToRestriction(entity: com.markduenas.visischeduler.data.local.RestrictionEntity): Restriction {
        return Restriction(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            type = RestrictionType.valueOf(entity.type),
            scope = RestrictionScope.valueOf(entity.scope),
            priority = entity.priority.toInt(),
            isActive = entity.isActive == 1L,
            effectiveFrom = LocalDate.parse(entity.effectiveFrom),
            effectiveUntil = entity.effectiveUntil?.let { LocalDate.parse(it) },
            timeConstraints = entity.timeConstraints?.let { json.decodeFromString(it) },
            visitorConstraints = entity.visitorConstraints?.let { json.decodeFromString(it) },
            beneficiaryConstraints = entity.beneficiaryConstraints?.let { json.decodeFromString(it) },
            facilityId = entity.facilityId,
            createdBy = entity.createdBy,
            createdAt = kotlinx.datetime.Instant.parse(entity.createdAt),
            updatedAt = kotlinx.datetime.Instant.parse(entity.updatedAt)
        )
    }
}
