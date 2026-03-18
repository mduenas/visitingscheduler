package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json

/**
 * Implementation of RestrictionRepository.
 */
class RestrictionRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : RestrictionRepository {

    override fun getActiveRestrictions(): Flow<List<Restriction>> = flowOf(emptyList())

    override fun getRestrictionsByType(type: RestrictionType): Flow<List<Restriction>> = flowOf(emptyList())

    override fun getRestrictionsByScope(scope: RestrictionScope): Flow<List<Restriction>> = flowOf(emptyList())

    override suspend fun getRestrictionById(restrictionId: String): Result<Restriction> = runCatching {
        throw Exception("Not implemented")
    }

    override fun getRestrictionsForVisitor(visitorId: String): Flow<List<Restriction>> = flowOf(emptyList())

    override fun getRestrictionsForBeneficiary(beneficiaryId: String): Flow<List<Restriction>> = flowOf(emptyList())

    override fun getRestrictionsForVisitorBeneficiaryPair(visitorId: String, beneficiaryId: String): Flow<List<Restriction>> = flowOf(emptyList())

    override fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>> = flowOf(emptyList())

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
        throw Exception("Not implemented")
    }

    override suspend fun updateRestriction(restriction: Restriction): Result<Restriction> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun deactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun reactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        throw Exception("Not implemented")
    }

    override suspend fun deleteRestriction(restrictionId: String): Result<Unit> = Result.success(Unit)

    override suspend fun checkVisitRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<Restriction>> = Result.success(emptyList())

    override suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<RestrictionViolation>> = Result.success(emptyList())

    override suspend fun syncRestrictions(): Result<Unit> = Result.success(Unit)
}
