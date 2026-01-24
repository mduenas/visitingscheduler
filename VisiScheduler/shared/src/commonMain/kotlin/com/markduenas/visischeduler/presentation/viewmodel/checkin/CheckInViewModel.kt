package com.markduenas.visischeduler.presentation.viewmodel.checkin

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.usecase.CheckInRequest
import com.markduenas.visischeduler.domain.usecase.CheckInUseCase
import com.markduenas.visischeduler.domain.usecase.GenerateQrCodeUseCase
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for check-in screen.
 */
class CheckInViewModel(
    private val checkInUseCase: CheckInUseCase,
    private val generateQrCodeUseCase: GenerateQrCodeUseCase,
    private val checkInRepository: CheckInRepository,
    private val visitRepository: VisitRepository
) : BaseViewModel<CheckInViewState>(CheckInViewState()) {

    private val _qrCodeData = MutableStateFlow<QrCodeData?>(null)
    val qrCodeData: StateFlow<QrCodeData?> = _qrCodeData.asStateFlow()

    /**
     * Load visit details and check-in status.
     */
    fun loadVisit(visitId: String) {
        launchSafe {
            updateState { copy(isLoading = true) }

            // Load visit details
            val visitResult = visitRepository.getVisitById(visitId)
            visitResult.fold(
                onSuccess = { visit ->
                    updateState { copy(visit = visit, isLoading = false) }

                    // Load active check-in if any
                    viewModelScope.launch {
                        checkInRepository.getActiveCheckIn(visitId).collect { checkIn ->
                            updateState { copy(activeCheckIn = checkIn, isCheckedIn = checkIn != null) }
                        }
                    }
                },
                onFailure = { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    showSnackbar(error.message ?: "Failed to load visit")
                }
            )
        }
    }

    /**
     * Generate QR code for the visit.
     */
    fun generateQrCode() {
        val visitId = currentState.visit?.id ?: return

        launchSafe {
            updateState { copy(isGeneratingQr = true) }

            val result = generateQrCodeUseCase(visitId)
            result.fold(
                onSuccess = { qrData ->
                    _qrCodeData.value = qrData
                    updateState { copy(isGeneratingQr = false) }
                },
                onFailure = { error ->
                    updateState { copy(isGeneratingQr = false, error = error.message) }
                    showSnackbar(error.message ?: "Failed to generate QR code")
                }
            )
        }
    }

    /**
     * Perform manual check-in.
     */
    fun checkIn(method: CheckInMethod = CheckInMethod.MANUAL) {
        val visitId = currentState.visit?.id ?: return

        launchSafe {
            updateState { copy(isCheckingIn = true) }

            val request = CheckInRequest(visitId = visitId, method = method)
            val result = checkInUseCase(request)

            result.fold(
                onSuccess = { checkIn ->
                    updateState {
                        copy(
                            activeCheckIn = checkIn,
                            isCheckedIn = true,
                            isCheckingIn = false
                        )
                    }
                    showSnackbar("Successfully checked in!")
                },
                onFailure = { error ->
                    updateState { copy(isCheckingIn = false, error = error.message) }
                    showSnackbar(error.message ?: "Check-in failed")
                }
            )
        }
    }

    /**
     * Navigate to check-out screen.
     */
    fun navigateToCheckOut() {
        val checkInId = currentState.activeCheckIn?.id ?: return
        navigate("checkout/$checkInId")
    }

    /**
     * Clear any error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Refresh visit and check-in status.
     */
    fun refresh() {
        currentState.visit?.id?.let { loadVisit(it) }
    }
}

/**
 * UI state for check-in screen.
 */
data class CheckInViewState(
    val visit: Visit? = null,
    val activeCheckIn: CheckIn? = null,
    val isCheckedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isCheckingIn: Boolean = false,
    val isGeneratingQr: Boolean = false,
    val error: String? = null
) {
    val canCheckIn: Boolean
        get() = visit != null && !isCheckedIn && !isCheckingIn

    val canGenerateQr: Boolean
        get() = visit != null && !isGeneratingQr

    val showCheckOutOption: Boolean
        get() = isCheckedIn && activeCheckIn != null
}
