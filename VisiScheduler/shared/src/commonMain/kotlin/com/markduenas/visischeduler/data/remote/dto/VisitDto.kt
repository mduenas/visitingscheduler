package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.AdditionalVisitor
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class VisitDto(
    val id: String,
    val beneficiaryId: String,
    val visitorId: String,
    val scheduledDate: String,
    val startTime: String,
    val endTime: String,
    val status: String,
    val visitType: String = "IN_PERSON",
    val purpose: String? = null,
    val notes: String? = null,
    val additionalVisitors: List<AdditionalVisitorDto> = emptyList(),
    val videoCallLink: String? = null,
    val videoCallPlatform: String? = null,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val denialReason: String? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): Visit {
        return Visit(
            id = id,
            beneficiaryId = beneficiaryId,
            visitorId = visitorId,
            scheduledDate = LocalDate.parse(scheduledDate),
            startTime = LocalTime.parse(startTime),
            endTime = LocalTime.parse(endTime),
            status = VisitStatus.valueOf(status),
            visitType = VisitType.valueOf(visitType),
            purpose = purpose,
            notes = notes,
            additionalVisitors = additionalVisitors.map { it.toDomain() },
            videoCallLink = videoCallLink,
            videoCallPlatform = videoCallPlatform,
            checkInTime = checkInTime?.let { Instant.parse(it) },
            checkOutTime = checkOutTime?.let { Instant.parse(it) },
            approvedBy = approvedBy,
            approvedAt = approvedAt?.let { Instant.parse(it) },
            denialReason = denialReason,
            cancellationReason = cancellationReason,
            cancelledBy = cancelledBy,
            cancelledAt = cancelledAt?.let { Instant.parse(it) },
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt)
        )
    }

    companion object {
        fun fromDomain(visit: Visit): VisitDto {
            return VisitDto(
                id = visit.id,
                beneficiaryId = visit.beneficiaryId,
                visitorId = visit.visitorId,
                scheduledDate = visit.scheduledDate.toString(),
                startTime = visit.startTime.toString(),
                endTime = visit.endTime.toString(),
                status = visit.status.name,
                visitType = visit.visitType.name,
                purpose = visit.purpose,
                notes = visit.notes,
                additionalVisitors = visit.additionalVisitors.map { AdditionalVisitorDto.fromDomain(it) },
                videoCallLink = visit.videoCallLink,
                videoCallPlatform = visit.videoCallPlatform,
                checkInTime = visit.checkInTime?.toString(),
                checkOutTime = visit.checkOutTime?.toString(),
                approvedBy = visit.approvedBy,
                approvedAt = visit.approvedAt?.toString(),
                denialReason = visit.denialReason,
                cancellationReason = visit.cancellationReason,
                cancelledBy = visit.cancelledBy,
                cancelledAt = visit.cancelledAt?.toString(),
                createdAt = visit.createdAt.toString(),
                updatedAt = visit.updatedAt.toString()
            )
        }
    }
}

@Serializable
data class AdditionalVisitorDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val relationship: String,
    val isMinor: Boolean = false,
    val age: Int? = null
) {
    fun toDomain(): AdditionalVisitor {
        return AdditionalVisitor(
            id = id,
            firstName = firstName,
            lastName = lastName,
            relationship = relationship,
            isMinor = isMinor,
            age = age
        )
    }

    companion object {
        fun fromDomain(visitor: AdditionalVisitor): AdditionalVisitorDto {
            return AdditionalVisitorDto(
                id = visitor.id,
                firstName = visitor.firstName,
                lastName = visitor.lastName,
                relationship = visitor.relationship,
                isMinor = visitor.isMinor,
                age = visitor.age
            )
        }
    }
}

@Serializable
data class VisitRequestDto(
    val beneficiaryId: String,
    val scheduledDate: String,
    val startTime: String,
    val endTime: String,
    val visitType: String = "IN_PERSON",
    val purpose: String? = null,
    val notes: String? = null,
    val additionalVisitors: List<AdditionalVisitorDto> = emptyList(),
    val videoCallLink: String? = null,
    val videoCallPlatform: String? = null
)
