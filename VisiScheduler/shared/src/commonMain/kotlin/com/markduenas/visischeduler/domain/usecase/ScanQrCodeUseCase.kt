package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.QrValidationResult
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.repository.CheckInRepository

/**
 * Use case for scanning QR codes and performing check-in.
 */
class ScanQrCodeUseCase(
    private val checkInRepository: CheckInRepository
) {
    /**
     * Validate a scanned QR code.
     * @param qrData The raw QR code data string
     * @return Result containing the QR validation result
     */
    suspend fun validate(qrData: String): Result<QrValidationResult> {
        if (qrData.isBlank()) {
            return Result.failure(
                ScanQrCodeException.InvalidQrData("QR code data is empty")
            )
        }

        return checkInRepository.validateQrCode(qrData)
    }

    /**
     * Validate and check in using a scanned QR code.
     * @param qrData The raw QR code data string
     * @return Result containing the ScanResult with validation and optional check-in
     */
    suspend operator fun invoke(qrData: String): Result<ScanResult> {
        // Validate the QR code
        val validationResult = validate(qrData)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        val qrValidation = validationResult.getOrNull()!!

        // Only proceed with check-in if validation was successful
        return when (qrValidation) {
            is QrValidationResult.Valid -> {
                // Perform check-in
                val checkInResult = checkInRepository.checkIn(
                    visitId = qrValidation.visit.id,
                    method = CheckInMethod.QR_CODE
                )

                checkInResult.fold(
                    onSuccess = { checkIn ->
                        Result.success(
                            ScanResult.CheckedIn(
                                visit = qrValidation.visit,
                                checkIn = checkIn
                            )
                        )
                    },
                    onFailure = { error ->
                        Result.success(
                            ScanResult.ValidationOnly(
                                validation = qrValidation,
                                checkInError = error.message
                            )
                        )
                    }
                )
            }
            else -> {
                Result.success(
                    ScanResult.ValidationOnly(
                        validation = qrValidation,
                        checkInError = null
                    )
                )
            }
        }
    }
}

/**
 * Result of scanning a QR code.
 */
sealed class ScanResult {
    /**
     * QR code was valid and check-in was successful.
     */
    data class CheckedIn(
        val visit: Visit,
        val checkIn: CheckIn
    ) : ScanResult()

    /**
     * QR code was validated but check-in was not performed or failed.
     */
    data class ValidationOnly(
        val validation: QrValidationResult,
        val checkInError: String?
    ) : ScanResult()
}

/**
 * Exceptions that can occur during QR code scanning.
 */
sealed class ScanQrCodeException(message: String) : Exception(message) {
    class InvalidQrData(message: String) : ScanQrCodeException(message)
    class ParseFailed(message: String) : ScanQrCodeException(message)
    class NetworkError(message: String) : ScanQrCodeException(message)
}
