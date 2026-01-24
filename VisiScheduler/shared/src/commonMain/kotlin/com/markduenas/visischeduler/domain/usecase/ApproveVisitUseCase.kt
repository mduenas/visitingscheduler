package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlinx.coroutines.flow.first

/**
 * Use case for approving or denying visit requests.
 */
class ApproveVisitUseCase(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository
) {
    /**
     * Approve a visit request.
     * @param visitId The ID of the visit to approve
     * @param notes Optional notes for the approval
     * @return Result containing the updated Visit or an error
     */
    suspend fun approve(visitId: String, notes: String? = null): Result<Visit> {
        // Get current user and verify permissions
        val currentUser = userRepository.currentUser.first()
            ?: return Result.failure(ApproveVisitException.NotAuthenticated("User not authenticated"))

        if (!currentUser.canApproveVisits()) {
            return Result.failure(
                ApproveVisitException.NotAuthorized(
                    "User role ${currentUser.role} is not authorized to approve visits"
                )
            )
        }

        // Get the visit
        val visitResult = visitRepository.getVisitById(visitId)
        if (visitResult.isFailure) {
            return Result.failure(
                ApproveVisitException.VisitNotFound("Visit with ID $visitId not found")
            )
        }

        val visit = visitResult.getOrNull()!!

        // Validate visit status
        if (visit.status != VisitStatus.PENDING) {
            return Result.failure(
                ApproveVisitException.InvalidStatus(
                    "Cannot approve visit with status ${visit.status}. Only PENDING visits can be approved."
                )
            )
        }

        // Check if secondary coordinator can approve this visit
        if (currentUser.role == Role.SECONDARY_COORDINATOR) {
            val canApprove = canSecondaryCoordinatorApprove(visit, currentUser)
            if (!canApprove) {
                return Result.failure(
                    ApproveVisitException.NotAuthorized(
                        "Secondary coordinators cannot approve this type of visit"
                    )
                )
            }
        }

        return visitRepository.approveVisit(visitId, notes)
    }

    /**
     * Deny a visit request.
     * @param visitId The ID of the visit to deny
     * @param reason The reason for denial (required)
     * @return Result containing the updated Visit or an error
     */
    suspend fun deny(visitId: String, reason: String): Result<Visit> {
        // Validate reason
        if (reason.isBlank()) {
            return Result.failure(
                ApproveVisitException.InvalidReason("Denial reason is required")
            )
        }

        if (reason.length < 10) {
            return Result.failure(
                ApproveVisitException.InvalidReason("Denial reason must be at least 10 characters")
            )
        }

        // Get current user and verify permissions
        val currentUser = userRepository.currentUser.first()
            ?: return Result.failure(ApproveVisitException.NotAuthenticated("User not authenticated"))

        if (!currentUser.canApproveVisits()) {
            return Result.failure(
                ApproveVisitException.NotAuthorized(
                    "User role ${currentUser.role} is not authorized to deny visits"
                )
            )
        }

        // Get the visit
        val visitResult = visitRepository.getVisitById(visitId)
        if (visitResult.isFailure) {
            return Result.failure(
                ApproveVisitException.VisitNotFound("Visit with ID $visitId not found")
            )
        }

        val visit = visitResult.getOrNull()!!

        // Validate visit status
        if (visit.status != VisitStatus.PENDING) {
            return Result.failure(
                ApproveVisitException.InvalidStatus(
                    "Cannot deny visit with status ${visit.status}. Only PENDING visits can be denied."
                )
            )
        }

        return visitRepository.denyVisit(visitId, reason)
    }

    /**
     * Bulk approve multiple visits.
     * @param visitIds List of visit IDs to approve
     * @param notes Optional notes for all approvals
     * @return Map of visitId to Result
     */
    suspend fun bulkApprove(
        visitIds: List<String>,
        notes: String? = null
    ): Map<String, Result<Visit>> {
        return visitIds.associateWith { visitId ->
            approve(visitId, notes)
        }
    }

    /**
     * Bulk deny multiple visits.
     * @param visitIds List of visit IDs to deny
     * @param reason Denial reason for all visits
     * @return Map of visitId to Result
     */
    suspend fun bulkDeny(
        visitIds: List<String>,
        reason: String
    ): Map<String, Result<Visit>> {
        return visitIds.associateWith { visitId ->
            deny(visitId, reason)
        }
    }

    private fun canSecondaryCoordinatorApprove(visit: Visit, coordinator: User): Boolean {
        // Secondary coordinators can only approve standard visits
        // Special events or visits with many additional visitors require primary coordinator
        return visit.additionalVisitors.size <= 2
    }
}

/**
 * Exceptions that can occur during visit approval/denial.
 */
sealed class ApproveVisitException(message: String) : Exception(message) {
    class NotAuthenticated(message: String) : ApproveVisitException(message)
    class NotAuthorized(message: String) : ApproveVisitException(message)
    class VisitNotFound(message: String) : ApproveVisitException(message)
    class InvalidStatus(message: String) : ApproveVisitException(message)
    class InvalidReason(message: String) : ApproveVisitException(message)
    class NetworkError(message: String) : ApproveVisitException(message)
}
