package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BeneficiaryDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String? = null,
    val facilityId: String,
    val roomNumber: String? = null,
    val status: String = "ACTIVE",
    val specialInstructions: String? = null,
    val maxVisitorsPerSlot: Int = 2,
    val maxVisitsPerDay: Int = 2,
    val maxVisitsPerWeek: Int = 7,
    val photoUrl: String? = null,
    val emergencyContact: EmergencyContactDto? = null,
    val restrictions: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String
) {
    fun toDomain(): Beneficiary {
        return Beneficiary(
            id = id,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth?.let { LocalDate.parse(it) },
            facilityId = facilityId,
            roomNumber = roomNumber,
            status = BeneficiaryStatus.valueOf(status),
            specialInstructions = specialInstructions,
            maxVisitorsPerSlot = maxVisitorsPerSlot,
            maxVisitsPerDay = maxVisitsPerDay,
            maxVisitsPerWeek = maxVisitsPerWeek,
            photoUrl = photoUrl,
            emergencyContact = emergencyContact?.toDomain(),
            restrictions = restrictions,
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt)
        )
    }

    companion object {
        fun fromDomain(beneficiary: Beneficiary): BeneficiaryDto {
            return BeneficiaryDto(
                id = beneficiary.id,
                firstName = beneficiary.firstName,
                lastName = beneficiary.lastName,
                dateOfBirth = beneficiary.dateOfBirth?.toString(),
                facilityId = beneficiary.facilityId,
                roomNumber = beneficiary.roomNumber,
                status = beneficiary.status.name,
                specialInstructions = beneficiary.specialInstructions,
                maxVisitorsPerSlot = beneficiary.maxVisitorsPerSlot,
                maxVisitsPerDay = beneficiary.maxVisitsPerDay,
                maxVisitsPerWeek = beneficiary.maxVisitsPerWeek,
                photoUrl = beneficiary.photoUrl,
                emergencyContact = beneficiary.emergencyContact?.let { EmergencyContactDto.fromDomain(it) },
                restrictions = beneficiary.restrictions,
                createdAt = beneficiary.createdAt.toString(),
                updatedAt = beneficiary.updatedAt.toString()
            )
        }
    }
}

@Serializable
data class EmergencyContactDto(
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val email: String? = null,
    val isPrimaryContact: Boolean = true
) {
    fun toDomain(): EmergencyContact {
        return EmergencyContact(
            name = name,
            relationship = relationship,
            phoneNumber = phoneNumber,
            email = email,
            isPrimaryContact = isPrimaryContact
        )
    }

    companion object {
        fun fromDomain(contact: EmergencyContact): EmergencyContactDto {
            return EmergencyContactDto(
                name = contact.name,
                relationship = contact.relationship,
                phoneNumber = contact.phoneNumber,
                email = contact.email,
                isPrimaryContact = contact.isPrimaryContact
            )
        }
    }
}
