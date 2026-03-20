package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.RestrictionDto
import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.RestrictionViolation
import com.markduenas.visischeduler.domain.repository.ViolationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Implementation of RestrictionRepository.
 */
class RestrictionRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : RestrictionRepository {

    override fun getActiveRestrictions(): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            emit(restrictions.filter { it.isActive })
        } catch (e: Exception) {}
    }

    override fun getRestrictionsByType(type: RestrictionType): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            emit(restrictions.filter { it.type == type && it.isActive })
        } catch (e: Exception) {}
    }

    override fun getRestrictionsByScope(scope: RestrictionScope): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            emit(restrictions.filter { it.scope == scope && it.isActive })
        } catch (e: Exception) {}
    }

    override suspend fun getRestrictionById(restrictionId: String): Result<Restriction> = runCatching {
        api.getRestrictionById(restrictionId).toDomain()
    }

    override fun getRestrictionsForVisitor(visitorId: String): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictionsForVisitor(visitorId).map { it.toDomain() }
            emit(restrictions.filter { it.isActive })
        } catch (e: Exception) {}
    }

    override fun getRestrictionsForBeneficiary(beneficiaryId: String): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictionsForBeneficiary(beneficiaryId).map { it.toDomain() }
            emit(restrictions.filter { it.isActive })
        } catch (e: Exception) {}
    }

    override fun getRestrictionsForVisitorBeneficiaryPair(
        visitorId: String,
        beneficiaryId: String
    ): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val visitorRestrictions = api.getRestrictionsForVisitor(visitorId).map { it.toDomain() }
            val beneficiaryRestrictions = api.getRestrictionsForBeneficiary(beneficiaryId).map { it.toDomain() }
            val pair = (visitorRestrictions + beneficiaryRestrictions)
                .distinctBy { it.id }
                .filter { it.isActive }
            emit(pair)
        } catch (e: Exception) {}
    }

    override fun getFacilityRestrictions(facilityId: String): Flow<List<Restriction>> = flow {
        emit(emptyList())
        try {
            val restrictions = api.getRestrictions().map { it.toDomain() }
            emit(restrictions.filter { it.facilityId == facilityId && it.isActive })
        } catch (e: Exception) {}
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
        val now = Clock.System.now()
        val placeholder = Restriction(
            id = "",
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
            createdBy = "",
            createdAt = now,
            updatedAt = now
        )
        api.createRestriction(RestrictionDto.fromDomain(placeholder)).toDomain()
    }

    override suspend fun updateRestriction(restriction: Restriction): Result<Restriction> = runCatching {
        api.updateRestriction(restriction.id, RestrictionDto.fromDomain(restriction)).toDomain()
    }

    override suspend fun deactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        api.deactivateRestriction(restrictionId).toDomain()
    }

    override suspend fun reactivateRestriction(restrictionId: String): Result<Restriction> = runCatching {
        api.reactivateRestriction(restrictionId).toDomain()
    }

    override suspend fun deleteRestriction(restrictionId: String): Result<Unit> = runCatching {
        api.deleteRestriction(restrictionId)
    }

    override suspend fun checkVisitRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<Restriction>> = runCatching {
        api.checkVisitRestrictions(
            visitorId = visitorId,
            beneficiaryId = beneficiaryId,
            visitDate = visitDate.toString(),
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            additionalVisitorCount = additionalVisitorCount
        ).map { it.toDomain() }
    }

    override suspend fun getViolationExplanations(
        visitorId: String,
        beneficiaryId: String,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): Result<List<RestrictionViolation>> = runCatching {
        val violated = api.checkVisitRestrictions(
            visitorId = visitorId,
            beneficiaryId = beneficiaryId,
            visitDate = visitDate.toString(),
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            additionalVisitorCount = additionalVisitorCount
        ).map { it.toDomain() }

        violated.map { restriction ->
            buildViolationExplanation(restriction, visitDate, startTime, endTime, additionalVisitorCount)
        }
    }

    private fun buildViolationExplanation(
        restriction: Restriction,
        visitDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        additionalVisitorCount: Int
    ): RestrictionViolation {
        val tc = restriction.timeConstraints
        val vc = restriction.visitorConstraints
        val bc = restriction.beneficiaryConstraints

        return when {
            tc?.blockedDays?.contains(visitDate.dayOfWeek) == true ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.TIME_VIOLATION,
                    message = "${restriction.name}: Visits are not allowed on ${visitDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}s.",
                    suggestedResolution = "Please choose a different day."
                )
            tc?.earliestStartTime != null && startTime < tc.earliestStartTime ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.TIME_VIOLATION,
                    message = "${restriction.name}: Start time must be no earlier than ${tc.earliestStartTime}.",
                    suggestedResolution = "Schedule the visit to start at ${tc.earliestStartTime} or later."
                )
            tc?.latestEndTime != null && endTime > tc.latestEndTime ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.TIME_VIOLATION,
                    message = "${restriction.name}: Visit must end by ${tc.latestEndTime}.",
                    suggestedResolution = "Shorten the visit so it ends by ${tc.latestEndTime}."
                )
            vc?.maxAdditionalGuests != null && additionalVisitorCount > vc.maxAdditionalGuests ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.CAPACITY_EXCEEDED,
                    message = "${restriction.name}: Maximum additional guests is ${vc.maxAdditionalGuests}.",
                    suggestedResolution = "Reduce the number of additional guests to ${vc.maxAdditionalGuests} or fewer."
                )
            bc?.maxSimultaneousVisitors != null ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.CAPACITY_EXCEEDED,
                    message = "${restriction.name}: Maximum simultaneous visitors is ${bc.maxSimultaneousVisitors}.",
                    suggestedResolution = "Choose a time slot with fewer concurrent visitors."
                )
            else ->
                RestrictionViolation(
                    restriction = restriction,
                    violationType = ViolationType.OTHER,
                    message = "${restriction.name}: This visit violates an active restriction.",
                    suggestedResolution = null
                )
        }
    }

    override suspend fun syncRestrictions(): Result<Unit> = Result.success(Unit)
}
