package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.ExpectedVisitorStatus
import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.VisitorBadge
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * DTO for CheckIn entity.
 */
@Serializable
data class CheckInDto(
    val id: String,
    val visitId: String,
    val checkInTime: String,
    val checkOutTime: String?,
    val method: String,
    val notes: String?,
    val rating: Int?,
    val moodLevel: Int? = null,
    val energyLevel: Int? = null
) {
    fun toDomain(): CheckIn {
        return CheckIn(
            id = id,
            visitId = visitId,
            checkInTime = Instant.parse(checkInTime),
            checkOutTime = checkOutTime?.let { Instant.parse(it) },
            method = CheckInMethod.valueOf(method),
            notes = notes,
            rating = rating,
            moodLevel = moodLevel,
            energyLevel = energyLevel
        )
    }

    companion object {
        fun fromDomain(checkIn: CheckIn): CheckInDto {
            return CheckInDto(
                id = checkIn.id,
                visitId = checkIn.visitId,
                checkInTime = checkIn.checkInTime.toString(),
                checkOutTime = checkIn.checkOutTime?.toString(),
                method = checkIn.method.name,
                notes = checkIn.notes,
                rating = checkIn.rating,
                moodLevel = checkIn.moodLevel,
                energyLevel = checkIn.energyLevel
            )
        }
    }
}

/**
 * DTO for QR code data.
 */
@Serializable
data class QrCodeDataDto(
    val visitId: String,
    val visitorId: String,
    val validFrom: String,
    val validUntil: String,
    val signature: String
) {
    fun toDomain(): QrCodeData {
        return QrCodeData(
            visitId = visitId,
            visitorId = visitorId,
            validFrom = Instant.parse(validFrom),
            validUntil = Instant.parse(validUntil),
            signature = signature
        )
    }

    companion object {
        fun fromDomain(qrCodeData: QrCodeData): QrCodeDataDto {
            return QrCodeDataDto(
                visitId = qrCodeData.visitId,
                visitorId = qrCodeData.visitorId,
                validFrom = qrCodeData.validFrom.toString(),
                validUntil = qrCodeData.validUntil.toString(),
                signature = qrCodeData.signature
            )
        }
    }
}

/**
 * Request for performing check-in.
 */
@Serializable
data class CheckInRequestDto(
    val method: String
)

/**
 * Request for performing check-out.
 */
@Serializable
data class CheckOutRequestDto(
    val notes: String?,
    val rating: Int?,
    val moodLevel: Int? = null,
    val energyLevel: Int? = null
)

/**
 * Request for validating QR code.
 */
@Serializable
data class ValidateQrRequestDto(
    val qrData: String
)

/**
 * Response from QR validation.
 */
@Serializable
data class QrValidationResponseDto(
    val status: String, // VALID, EXPIRED, NOT_YET_VALID, INVALID_SIGNATURE, VISIT_NOT_FOUND, ALREADY_CHECKED_IN, VISIT_CANCELLED
    val visit: VisitDto? = null,
    val checkIn: CheckInDto? = null,
    val expiredAt: String? = null,
    val validFrom: String? = null,
    val message: String? = null
)

/**
 * DTO for expected visitor.
 */
@Serializable
data class ExpectedVisitorDto(
    val visit: VisitDto,
    val visitorName: String,
    val visitorPhotoUrl: String?,
    val beneficiaryName: String,
    val beneficiaryRoom: String?,
    val checkInStatus: String
) {
    fun toDomain(): ExpectedVisitor {
        return ExpectedVisitor(
            visit = visit.toDomain(),
            visitorName = visitorName,
            visitorPhotoUrl = visitorPhotoUrl,
            beneficiaryName = beneficiaryName,
            beneficiaryRoom = beneficiaryRoom,
            checkInStatus = ExpectedVisitorStatus.valueOf(checkInStatus)
        )
    }
}

/**
 * DTO for visitor badge.
 */
@Serializable
data class VisitorBadgeDto(
    val visit: VisitDto,
    val visitorName: String,
    val visitorPhotoUrl: String?,
    val beneficiaryName: String,
    val beneficiaryRoom: String?,
    val checkInTime: String,
    val validUntil: String,
    val qrCodeData: QrCodeDataDto,
    val badgeNumber: String
) {
    fun toDomain(): VisitorBadge {
        return VisitorBadge(
            visit = visit.toDomain(),
            visitorName = visitorName,
            visitorPhotoUrl = visitorPhotoUrl,
            beneficiaryName = beneficiaryName,
            beneficiaryRoom = beneficiaryRoom,
            checkInTime = Instant.parse(checkInTime),
            validUntil = Instant.parse(validUntil),
            qrCodeData = qrCodeData.toDomain(),
            badgeNumber = badgeNumber
        )
    }
}

/**
 * DTO for check-in statistics.
 */
@Serializable
data class CheckInStatisticsDto(
    val totalCheckIns: Int,
    val qrCodeCheckIns: Int,
    val manualCheckIns: Int,
    val automaticCheckIns: Int,
    val averageVisitDurationMinutes: Int,
    val averageRating: Float?,
    val onTimePercentage: Float,
    val latePercentage: Float,
    val noShowCount: Int
)
