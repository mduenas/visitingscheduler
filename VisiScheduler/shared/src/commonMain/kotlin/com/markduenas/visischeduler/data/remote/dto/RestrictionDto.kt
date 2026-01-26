package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.ApprovalLevel
import com.markduenas.visischeduler.domain.entities.BeneficiaryConstraints
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.entities.TimeConstraints
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.entities.VisitorConstraints
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class RestrictionDto(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val scope: String,
    val priority: Int = 0,
    val isActive: Boolean = true,
    val effectiveFrom: String,
    val effectiveUntil: String? = null,
    val timeConstraints: TimeConstraintsDto? = null,
    val visitorConstraints: VisitorConstraintsDto? = null,
    val beneficiaryConstraints: BeneficiaryConstraintsDto? = null,
    val facilityId: String? = null,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): Restriction {
        return Restriction(
            id = id,
            name = name,
            description = description,
            type = RestrictionType.valueOf(type),
            scope = RestrictionScope.valueOf(scope),
            priority = priority,
            isActive = isActive,
            effectiveFrom = LocalDate.parse(effectiveFrom),
            effectiveUntil = effectiveUntil?.let { LocalDate.parse(it) },
            timeConstraints = timeConstraints?.toDomain(),
            visitorConstraints = visitorConstraints?.toDomain(),
            beneficiaryConstraints = beneficiaryConstraints?.toDomain(),
            facilityId = facilityId,
            createdBy = createdBy,
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt)
        )
    }

    companion object {
        fun fromDomain(restriction: Restriction): RestrictionDto {
            return RestrictionDto(
                id = restriction.id,
                name = restriction.name,
                description = restriction.description,
                type = restriction.type.name,
                scope = restriction.scope.name,
                priority = restriction.priority,
                isActive = restriction.isActive,
                effectiveFrom = restriction.effectiveFrom.toString(),
                effectiveUntil = restriction.effectiveUntil?.toString(),
                timeConstraints = restriction.timeConstraints?.let { TimeConstraintsDto.fromDomain(it) },
                visitorConstraints = restriction.visitorConstraints?.let { VisitorConstraintsDto.fromDomain(it) },
                beneficiaryConstraints = restriction.beneficiaryConstraints?.let { BeneficiaryConstraintsDto.fromDomain(it) },
                facilityId = restriction.facilityId,
                createdBy = restriction.createdBy,
                createdAt = restriction.createdAt.toString(),
                updatedAt = restriction.updatedAt.toString()
            )
        }
    }
}

@Serializable
data class TimeConstraintsDto(
    val allowedDays: List<String>? = null,
    val blockedDays: List<String>? = null,
    val earliestStartTime: String? = null,
    val latestEndTime: String? = null,
    val maxDurationMinutes: Int? = null,
    val minAdvanceBookingHours: Int? = null,
    val maxAdvanceBookingDays: Int? = null,
    val requiredGapBetweenVisitsHours: Int? = null
) {
    fun toDomain(): TimeConstraints {
        return TimeConstraints(
            allowedDays = allowedDays?.map { DayOfWeek.valueOf(it) },
            blockedDays = blockedDays?.map { DayOfWeek.valueOf(it) },
            earliestStartTime = earliestStartTime?.let { LocalTime.parse(it) },
            latestEndTime = latestEndTime?.let { LocalTime.parse(it) },
            maxDurationMinutes = maxDurationMinutes,
            minAdvanceBookingHours = minAdvanceBookingHours,
            maxAdvanceBookingDays = maxAdvanceBookingDays,
            requiredGapBetweenVisitsHours = requiredGapBetweenVisitsHours
        )
    }

    companion object {
        fun fromDomain(constraints: TimeConstraints): TimeConstraintsDto {
            return TimeConstraintsDto(
                allowedDays = constraints.allowedDays?.map { it.name },
                blockedDays = constraints.blockedDays?.map { it.name },
                earliestStartTime = constraints.earliestStartTime?.toString(),
                latestEndTime = constraints.latestEndTime?.toString(),
                maxDurationMinutes = constraints.maxDurationMinutes,
                minAdvanceBookingHours = constraints.minAdvanceBookingHours,
                maxAdvanceBookingDays = constraints.maxAdvanceBookingDays,
                requiredGapBetweenVisitsHours = constraints.requiredGapBetweenVisitsHours
            )
        }
    }
}

@Serializable
data class VisitorConstraintsDto(
    val blockedVisitorIds: List<String>? = null,
    val allowedVisitorIds: List<String>? = null,
    val maxVisitsPerDay: Int? = null,
    val maxVisitsPerWeek: Int? = null,
    val maxVisitsPerMonth: Int? = null,
    val requiredApprovalLevel: String? = null,
    val requiresEscort: Boolean = false,
    val canBringGuests: Boolean = true,
    val maxAdditionalGuests: Int? = null
) {
    fun toDomain(): VisitorConstraints {
        return VisitorConstraints(
            blockedVisitorIds = blockedVisitorIds,
            allowedVisitorIds = allowedVisitorIds,
            maxVisitsPerDay = maxVisitsPerDay,
            maxVisitsPerWeek = maxVisitsPerWeek,
            maxVisitsPerMonth = maxVisitsPerMonth,
            requiredApprovalLevel = requiredApprovalLevel?.let { ApprovalLevel.valueOf(it) },
            requiresEscort = requiresEscort,
            canBringGuests = canBringGuests,
            maxAdditionalGuests = maxAdditionalGuests
        )
    }

    companion object {
        fun fromDomain(constraints: VisitorConstraints): VisitorConstraintsDto {
            return VisitorConstraintsDto(
                blockedVisitorIds = constraints.blockedVisitorIds,
                allowedVisitorIds = constraints.allowedVisitorIds,
                maxVisitsPerDay = constraints.maxVisitsPerDay,
                maxVisitsPerWeek = constraints.maxVisitsPerWeek,
                maxVisitsPerMonth = constraints.maxVisitsPerMonth,
                requiredApprovalLevel = constraints.requiredApprovalLevel?.name,
                requiresEscort = constraints.requiresEscort,
                canBringGuests = constraints.canBringGuests,
                maxAdditionalGuests = constraints.maxAdditionalGuests
            )
        }
    }
}

@Serializable
data class BeneficiaryConstraintsDto(
    val maxSimultaneousVisitors: Int? = null,
    val maxVisitsPerDay: Int? = null,
    val maxVisitsPerWeek: Int? = null,
    val restPeriodHours: Int? = null,
    val allowedVisitTypes: List<String>? = null,
    val requiresMedicalClearance: Boolean = false,
    val specialInstructions: String? = null
) {
    fun toDomain(): BeneficiaryConstraints {
        return BeneficiaryConstraints(
            maxSimultaneousVisitors = maxSimultaneousVisitors,
            maxVisitsPerDay = maxVisitsPerDay,
            maxVisitsPerWeek = maxVisitsPerWeek,
            restPeriodHours = restPeriodHours,
            allowedVisitTypes = allowedVisitTypes?.map { VisitType.valueOf(it) },
            requiresMedicalClearance = requiresMedicalClearance,
            specialInstructions = specialInstructions
        )
    }

    companion object {
        fun fromDomain(constraints: BeneficiaryConstraints): BeneficiaryConstraintsDto {
            return BeneficiaryConstraintsDto(
                maxSimultaneousVisitors = constraints.maxSimultaneousVisitors,
                maxVisitsPerDay = constraints.maxVisitsPerDay,
                maxVisitsPerWeek = constraints.maxVisitsPerWeek,
                restPeriodHours = constraints.restPeriodHours,
                allowedVisitTypes = constraints.allowedVisitTypes?.map { it.name },
                requiresMedicalClearance = constraints.requiresMedicalClearance,
                specialInstructions = constraints.specialInstructions
            )
        }
    }
}
