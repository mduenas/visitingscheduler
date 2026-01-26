package com.markduenas.visischeduler.domain.entities

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a beneficiary who receives visits in the VisiScheduler system.
 */
@Serializable
data class Beneficiary(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate? = null,
    val facilityId: String,
    val roomNumber: String? = null,
    val status: BeneficiaryStatus = BeneficiaryStatus.ACTIVE,
    val specialInstructions: String? = null,
    val maxVisitorsPerSlot: Int = 2,
    val maxVisitsPerDay: Int = 2,
    val maxVisitsPerWeek: Int = 7,
    val photoUrl: String? = null,
    val emergencyContact: EmergencyContact? = null,
    val restrictions: List<String> = emptyList(),
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
) {
    val fullName: String
        get() = "$firstName $lastName"
}

/**
 * Status of a beneficiary in the system.
 */
@Serializable
enum class BeneficiaryStatus {
    /** Active and can receive visits */
    ACTIVE,
    /** Temporarily unavailable for visits */
    TEMPORARILY_UNAVAILABLE,
    /** On medical hold - no visits allowed */
    MEDICAL_HOLD,
    /** Transferred to another facility */
    TRANSFERRED,
    /** No longer in the system */
    INACTIVE
}

/**
 * Emergency contact information for a beneficiary.
 */
@Serializable
data class EmergencyContact(
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val email: String? = null,
    val isPrimaryContact: Boolean = true
)
