package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.repository.CheckInRepository

/**
 * Use case for checking out from a visit.
 */
class CheckOutUseCase(
    private val checkInRepository: CheckInRepository
) {
    /**
     * Check out from a visit.
     * @param request The check-out request
     * @return Result containing the updated CheckIn record or an error
     */
    suspend operator fun invoke(request: CheckOutRequest): Result<CheckIn> {
        // Validate rating if provided
        if (request.rating != null && request.rating !in 1..5) {
            return Result.failure(
                CheckOutException.InvalidRating("Rating must be between 1 and 5")
            )
        }

        // Get the check-in record
        val checkInResult = checkInRepository.getCheckInById(request.checkInId)
        if (checkInResult.isFailure) {
            return Result.failure(
                CheckOutException.CheckInNotFound("Check-in not found: ${request.checkInId}")
            )
        }

        val checkIn = checkInResult.getOrNull()!!

        // Verify not already checked out
        if (checkIn.isCheckedOut) {
            return Result.failure(
                CheckOutException.AlreadyCheckedOut(
                    "Already checked out at ${checkIn.checkOutTime}"
                )
            )
        }

        // Validate notes length
        if (request.notes != null && request.notes.length > MAX_NOTES_LENGTH) {
            return Result.failure(
                CheckOutException.NotesTooLong(
                    "Notes cannot exceed $MAX_NOTES_LENGTH characters"
                )
            )
        }

        // Perform check-out
        return checkInRepository.checkOut(
            checkInId = request.checkInId,
            notes = request.notes,
            rating = request.rating,
            moodLevel = request.moodLevel,
            energyLevel = request.energyLevel
        )
    }

    companion object {
        private const val MAX_NOTES_LENGTH = 500
    }
}

/**
 * Request data for checking out.
 */
data class CheckOutRequest(
    val checkInId: String,
    val notes: String? = null,
    val rating: Int? = null, // 1-5
    val moodLevel: Int? = null, // 1-5: 1=very sad, 5=very happy
    val energyLevel: Int? = null // 1-5: 1=exhausted, 5=very energetic
)

/**
 * Exceptions that can occur during check-out.
 */
sealed class CheckOutException(message: String) : Exception(message) {
    class CheckInNotFound(message: String) : CheckOutException(message)
    class AlreadyCheckedOut(message: String) : CheckOutException(message)
    class InvalidRating(message: String) : CheckOutException(message)
    class NotesTooLong(message: String) : CheckOutException(message)
    class CheckOutFailed(message: String) : CheckOutException(message)
}
