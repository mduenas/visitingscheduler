package com.markduenas.visischeduler.presentation.viewmodel.settings

import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/**
 * UI State for adding/editing a beneficiary.
 */
data class AddBeneficiaryUiState(
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val facilityId: String = "default_facility",
    val roomNumber: String = "",
    val specialInstructions: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false,
    val beneficiaryId: String? = null
)

/**
 * ViewModel for adding or editing a beneficiary.
 */
class AddBeneficiaryViewModel(
    private val beneficiaryRepository: BeneficiaryRepository
) : BaseViewModel<AddBeneficiaryUiState>(AddBeneficiaryUiState()) {

    /**
     * Load beneficiary for editing.
     */
    fun loadBeneficiary(id: String) {
        updateState { copy(isLoading = true, isEditMode = true, beneficiaryId = id) }
        
        launchSafe {
            beneficiaryRepository.getBeneficiaryById(id)
                .onSuccess { beneficiary ->
                    updateState {
                        copy(
                            firstName = beneficiary.firstName,
                            lastName = beneficiary.lastName,
                            dateOfBirth = beneficiary.dateOfBirth?.toString() ?: "",
                            facilityId = beneficiary.facilityId,
                            roomNumber = beneficiary.roomNumber ?: "",
                            specialInstructions = beneficiary.specialInstructions ?: "",
                            emergencyContactName = beneficiary.emergencyContact?.name ?: "",
                            emergencyContactPhone = beneficiary.emergencyContact?.phoneNumber ?: "",
                            emergencyContactRelationship = beneficiary.emergencyContact?.relationship ?: "",
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    // Input change handlers
    fun onFirstNameChange(value: String) = updateState { copy(firstName = value) }
    fun onLastNameChange(value: String) = updateState { copy(lastName = value) }
    fun onDobChange(value: String) = updateState { copy(dateOfBirth = value) }
    fun onRoomChange(value: String) = updateState { copy(roomNumber = value) }
    fun onInstructionsChange(value: String) = updateState { copy(specialInstructions = value) }
    fun onContactNameChange(value: String) = updateState { copy(emergencyContactName = value) }
    fun onContactPhoneChange(value: String) = updateState { copy(emergencyContactPhone = value) }
    fun onContactRelChange(value: String) = updateState { copy(emergencyContactRelationship = value) }

    /**
     * Save the beneficiary.
     */
    fun saveBeneficiary() {
        if (!validate()) return

        updateState { copy(isSaving = true, error = null) }

        val now = Clock.System.now()
        val dob = try {
            if (currentState.dateOfBirth.isNotBlank()) LocalDate.parse(currentState.dateOfBirth) else null
        } catch (e: Exception) {
            updateState { copy(isSaving = false, error = "Invalid date format (use YYYY-MM-DD)") }
            return
        }

        val beneficiary = Beneficiary(
            id = currentState.beneficiaryId ?: "",
            firstName = currentState.firstName,
            lastName = currentState.lastName,
            dateOfBirth = dob,
            facilityId = currentState.facilityId,
            roomNumber = currentState.roomNumber.takeIf { it.isNotBlank() },
            status = BeneficiaryStatus.ACTIVE,
            specialInstructions = currentState.specialInstructions.takeIf { it.isNotBlank() },
            emergencyContact = EmergencyContact(
                name = currentState.emergencyContactName,
                relationship = currentState.emergencyContactRelationship,
                phoneNumber = currentState.emergencyContactPhone
            ),
            createdAt = now,
            updatedAt = now
        )

        launchSafe {
            val result = if (currentState.isEditMode) {
                beneficiaryRepository.updateBeneficiary(beneficiary)
            } else {
                beneficiaryRepository.createBeneficiary(beneficiary)
            }

            result.fold(
                onSuccess = {
                    updateState { copy(isSaving = false, saveSuccess = true) }
                    showSnackbar("Beneficiary saved successfully")
                },
                onFailure = { error ->
                    updateState { copy(isSaving = false, error = error.message) }
                }
            )
        }
    }

    private fun validate(): Boolean {
        return when {
            currentState.firstName.isBlank() -> {
                updateState { copy(error = "First name is required") }
                false
            }
            currentState.lastName.isBlank() -> {
                updateState { copy(error = "Last name is required") }
                false
            }
            else -> true
        }
    }
}
