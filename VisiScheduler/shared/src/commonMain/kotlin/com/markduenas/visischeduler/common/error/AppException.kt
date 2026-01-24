package com.markduenas.visischeduler.common.error

/**
 * Base sealed class for all application exceptions.
 * Provides a unified error handling strategy across the application.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Network-related exceptions (connectivity, timeouts, etc.)
     */
    sealed class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause) {

        data class NoConnection(
            override val message: String = "No internet connection available"
        ) : NetworkException(message)

        data class Timeout(
            override val message: String = "Request timed out"
        ) : NetworkException(message)

        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error: $statusCode"
        ) : NetworkException(message)

        data class ClientError(
            val statusCode: Int,
            override val message: String = "Client error: $statusCode"
        ) : NetworkException(message)
    }

    /**
     * Authentication-related exceptions
     */
    sealed class AuthException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause) {

        data class InvalidCredentials(
            override val message: String = "Invalid email or password"
        ) : AuthException(message)

        data class TokenExpired(
            override val message: String = "Session expired. Please login again"
        ) : AuthException(message)

        data class Unauthorized(
            override val message: String = "You are not authorized to perform this action"
        ) : AuthException(message)

        data class MfaRequired(
            override val message: String = "Multi-factor authentication required"
        ) : AuthException(message)

        data class MfaFailed(
            override val message: String = "Invalid verification code"
        ) : AuthException(message)

        data class BiometricFailed(
            override val message: String = "Biometric authentication failed"
        ) : AuthException(message)

        data class AccountLocked(
            override val message: String = "Account is locked. Please contact support"
        ) : AuthException(message)

        data class AccountNotVerified(
            override val message: String = "Please verify your email address"
        ) : AuthException(message)
    }

    /**
     * Validation-related exceptions
     */
    sealed class ValidationException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause) {

        data class InvalidField(
            val fieldName: String,
            override val message: String = "Invalid value for $fieldName"
        ) : ValidationException(message)

        data class RequiredField(
            val fieldName: String,
            override val message: String = "$fieldName is required"
        ) : ValidationException(message)

        data class InvalidFormat(
            val fieldName: String,
            val expectedFormat: String,
            override val message: String = "$fieldName must be in format: $expectedFormat"
        ) : ValidationException(message)

        data class OutOfRange(
            val fieldName: String,
            val min: Any?,
            val max: Any?,
            override val message: String = "$fieldName must be between $min and $max"
        ) : ValidationException(message)
    }

    /**
     * Business rule-related exceptions
     */
    sealed class BusinessRuleException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause) {

        data class SchedulingConflict(
            override val message: String = "The requested time slot conflicts with an existing visit"
        ) : BusinessRuleException(message)

        data class BufferTimeViolation(
            val requiredBufferMinutes: Int,
            override val message: String = "A buffer of at least $requiredBufferMinutes minutes is required between visits"
        ) : BusinessRuleException(message)

        data class FatigueLimitExceeded(
            override val message: String = "Maximum daily visit limit has been reached"
        ) : BusinessRuleException(message)

        data class CapacityExceeded(
            override val message: String = "Maximum visitor capacity has been reached for this time slot"
        ) : BusinessRuleException(message)

        data class RestrictionViolation(
            val restrictionName: String,
            override val message: String = "Visit violates restriction: $restrictionName"
        ) : BusinessRuleException(message)

        data class VisitorNotApproved(
            override val message: String = "Visitor is not approved to schedule visits"
        ) : BusinessRuleException(message)

        data class VisitNotModifiable(
            override val message: String = "This visit can no longer be modified"
        ) : BusinessRuleException(message)

        data class InsufficientPermissions(
            val requiredRole: String,
            override val message: String = "Requires $requiredRole role to perform this action"
        ) : BusinessRuleException(message)
    }

    /**
     * Data-related exceptions
     */
    sealed class DataException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause) {

        data class NotFound(
            val entityType: String,
            val entityId: String? = null,
            override val message: String = "$entityType${entityId?.let { " with ID $it" } ?: ""} not found"
        ) : DataException(message)

        data class DatabaseError(
            override val message: String = "Database operation failed",
            override val cause: Throwable? = null
        ) : DataException(message, cause)

        data class SerializationError(
            override val message: String = "Failed to process data",
            override val cause: Throwable? = null
        ) : DataException(message, cause)

        data class SyncError(
            override val message: String = "Failed to sync data",
            override val cause: Throwable? = null
        ) : DataException(message, cause)
    }

    /**
     * Unknown or unexpected exceptions
     */
    data class UnknownException(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    /**
     * Returns a user-friendly error message suitable for display.
     */
    open fun getUserMessage(): String = message

    /**
     * Returns an error code for logging/analytics purposes.
     */
    fun getErrorCode(): String = when (this) {
        is NetworkException.NoConnection -> "NET_001"
        is NetworkException.Timeout -> "NET_002"
        is NetworkException.ServerError -> "NET_003"
        is NetworkException.ClientError -> "NET_004"
        is AuthException.InvalidCredentials -> "AUTH_001"
        is AuthException.TokenExpired -> "AUTH_002"
        is AuthException.Unauthorized -> "AUTH_003"
        is AuthException.MfaRequired -> "AUTH_004"
        is AuthException.MfaFailed -> "AUTH_005"
        is AuthException.BiometricFailed -> "AUTH_006"
        is AuthException.AccountLocked -> "AUTH_007"
        is AuthException.AccountNotVerified -> "AUTH_008"
        is ValidationException.InvalidField -> "VAL_001"
        is ValidationException.RequiredField -> "VAL_002"
        is ValidationException.InvalidFormat -> "VAL_003"
        is ValidationException.OutOfRange -> "VAL_004"
        is BusinessRuleException.SchedulingConflict -> "BUS_001"
        is BusinessRuleException.BufferTimeViolation -> "BUS_002"
        is BusinessRuleException.FatigueLimitExceeded -> "BUS_003"
        is BusinessRuleException.CapacityExceeded -> "BUS_004"
        is BusinessRuleException.RestrictionViolation -> "BUS_005"
        is BusinessRuleException.VisitorNotApproved -> "BUS_006"
        is BusinessRuleException.VisitNotModifiable -> "BUS_007"
        is BusinessRuleException.InsufficientPermissions -> "BUS_008"
        is DataException.NotFound -> "DATA_001"
        is DataException.DatabaseError -> "DATA_002"
        is DataException.SerializationError -> "DATA_003"
        is DataException.SyncError -> "DATA_004"
        is UnknownException -> "UNK_001"
    }
}
