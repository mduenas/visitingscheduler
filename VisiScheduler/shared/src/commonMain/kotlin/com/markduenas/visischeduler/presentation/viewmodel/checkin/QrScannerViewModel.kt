package com.markduenas.visischeduler.presentation.viewmodel.checkin

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.QrValidationResult
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.usecase.ScanQrCodeUseCase
import com.markduenas.visischeduler.domain.usecase.ScanResult
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.datetime.Instant

/**
 * ViewModel for QR scanner screen.
 */
class QrScannerViewModel(
    private val scanQrCodeUseCase: ScanQrCodeUseCase
) : BaseViewModel<QrScannerViewState>(QrScannerViewState()) {

    /**
     * Start scanning.
     */
    fun startScanning() {
        updateState { copy(isScanning = true, scanResult = null, error = null) }
    }

    /**
     * Stop scanning.
     */
    fun stopScanning() {
        updateState { copy(isScanning = false) }
    }

    /**
     * Toggle flashlight.
     */
    fun toggleFlash() {
        updateState { copy(isFlashEnabled = !isFlashEnabled) }
    }

    /**
     * Process scanned QR code data.
     */
    fun processQrCode(qrData: String) {
        if (currentState.isProcessing) return

        launchSafe {
            updateState { copy(isProcessing = true, isScanning = false) }

            val result = scanQrCodeUseCase(qrData)

            result.fold(
                onSuccess = { scanResult ->
                    when (scanResult) {
                        is ScanResult.CheckedIn -> {
                            updateState {
                                copy(
                                    isProcessing = false,
                                    scanResult = scanResult,
                                    scannedVisit = scanResult.visit,
                                    checkIn = scanResult.checkIn,
                                    successMessage = "Check-in successful!"
                                )
                            }
                            showSnackbar("Successfully checked in!")
                        }
                        is ScanResult.ValidationOnly -> {
                            handleValidationResult(scanResult.validation, scanResult.checkInError)
                        }
                    }
                },
                onFailure = { error ->
                    updateState {
                        copy(
                            isProcessing = false,
                            error = error.message ?: "Failed to process QR code"
                        )
                    }
                    showSnackbar(error.message ?: "Scan failed")
                }
            )
        }
    }

    private fun handleValidationResult(validation: QrValidationResult, checkInError: String?) {
        when (validation) {
            is QrValidationResult.Valid -> {
                // Validation passed but check-in failed
                updateState {
                    copy(
                        isProcessing = false,
                        scannedVisit = validation.visit,
                        error = checkInError ?: "Check-in failed"
                    )
                }
            }
            is QrValidationResult.Expired -> {
                updateState {
                    copy(
                        isProcessing = false,
                        error = "QR code has expired at ${validation.expiredAt}"
                    )
                }
            }
            is QrValidationResult.NotYetValid -> {
                updateState {
                    copy(
                        isProcessing = false,
                        error = "QR code is not valid yet. Valid from: ${validation.validFrom}"
                    )
                }
            }
            is QrValidationResult.InvalidSignature -> {
                updateState {
                    copy(
                        isProcessing = false,
                        error = "Invalid QR code: ${validation.message}"
                    )
                }
            }
            is QrValidationResult.VisitNotFound -> {
                updateState {
                    copy(
                        isProcessing = false,
                        error = "Visit not found: ${validation.visitId}"
                    )
                }
            }
            is QrValidationResult.AlreadyCheckedIn -> {
                updateState {
                    copy(
                        isProcessing = false,
                        checkIn = validation.checkIn,
                        error = "Already checked in at ${validation.checkIn.checkInTime}"
                    )
                }
            }
            is QrValidationResult.VisitCancelled -> {
                updateState {
                    copy(
                        isProcessing = false,
                        scannedVisit = validation.visit,
                        error = "Visit has been cancelled"
                    )
                }
            }
        }
    }

    /**
     * Process manual entry code.
     */
    fun processManualEntry(code: String) {
        if (code.isBlank()) {
            showSnackbar("Please enter a valid code")
            return
        }
        processQrCode(code)
    }

    /**
     * Show manual entry dialog.
     */
    fun showManualEntry() {
        updateState { copy(showManualEntryDialog = true, isScanning = false) }
    }

    /**
     * Hide manual entry dialog.
     */
    fun hideManualEntry() {
        updateState { copy(showManualEntryDialog = false) }
    }

    /**
     * Clear the scan result and resume scanning.
     */
    fun clearAndRescan() {
        updateState {
            copy(
                scanResult = null,
                scannedVisit = null,
                checkIn = null,
                error = null,
                successMessage = null,
                isScanning = true
            )
        }
    }

    /**
     * Navigate to visit details.
     */
    fun viewVisitDetails() {
        val visitId = currentState.scannedVisit?.id ?: return
        navigate("visit/$visitId")
    }

    /**
     * Navigate to check-out if already checked in.
     */
    fun navigateToCheckOut() {
        val checkInId = currentState.checkIn?.id ?: return
        navigate("checkout/$checkInId")
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}

/**
 * UI state for QR scanner screen.
 */
data class QrScannerViewState(
    val isScanning: Boolean = true,
    val isProcessing: Boolean = false,
    val isFlashEnabled: Boolean = false,
    val showManualEntryDialog: Boolean = false,
    val scanResult: ScanResult? = null,
    val scannedVisit: Visit? = null,
    val checkIn: CheckIn? = null,
    val successMessage: String? = null,
    val error: String? = null
) {
    val hasResult: Boolean
        get() = scanResult != null || scannedVisit != null || error != null

    val isCheckInSuccess: Boolean
        get() = scanResult is ScanResult.CheckedIn

    val canViewDetails: Boolean
        get() = scannedVisit != null

    val canCheckOut: Boolean
        get() = checkIn != null && !checkIn.isCheckedOut
}
