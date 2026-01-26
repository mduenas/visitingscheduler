package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Use case for generating QR codes for visit check-in.
 */
class GenerateQrCodeUseCase(
    private val checkInRepository: CheckInRepository,
    private val visitRepository: VisitRepository
) {
    /**
     * Generate a QR code for a visit.
     * @param visitId The ID of the visit
     * @return Result containing the QR code data or an error
     */
    suspend operator fun invoke(visitId: String): Result<QrCodeData> {
        // Get the visit
        val visitResult = visitRepository.getVisitById(visitId)
        if (visitResult.isFailure) {
            return Result.failure(
                GenerateQrCodeException.VisitNotFound("Visit not found: $visitId")
            )
        }

        val visit = visitResult.getOrNull()!!

        // Validate visit status
        if (visit.status != VisitStatus.APPROVED) {
            return Result.failure(
                GenerateQrCodeException.InvalidVisitStatus(
                    "Cannot generate QR code for visit with status: ${visit.status}. " +
                    "Only approved visits can have QR codes generated."
                )
            )
        }

        // Validate that visit is for today or future
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (visit.scheduledDate < today) {
            return Result.failure(
                GenerateQrCodeException.VisitInPast(
                    "Cannot generate QR code for past visit. " +
                    "Visit was scheduled for ${visit.scheduledDate}."
                )
            )
        }

        // Generate QR code
        return checkInRepository.generateQrCode(visitId)
    }
}

/**
 * Exceptions that can occur during QR code generation.
 */
sealed class GenerateQrCodeException(message: String) : Exception(message) {
    class VisitNotFound(message: String) : GenerateQrCodeException(message)
    class InvalidVisitStatus(message: String) : GenerateQrCodeException(message)
    class VisitInPast(message: String) : GenerateQrCodeException(message)
    class GenerationFailed(message: String) : GenerateQrCodeException(message)
}
