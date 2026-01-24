package com.markduenas.visischeduler.presentation.state

import com.markduenas.visischeduler.common.error.AppException

/**
 * Base sealed interface for UI state management following UDF (Unidirectional Data Flow).
 * All screen-specific UI states should extend this interface.
 *
 * @param T The type of data held by the state
 */
sealed interface UiState<out T> {
    /**
     * Initial state before any data is loaded.
     */
    data object Idle : UiState<Nothing>

    /**
     * State indicating data is being loaded.
     */
    data object Loading : UiState<Nothing>

    /**
     * State indicating data was loaded successfully.
     *
     * @param data The loaded data
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * State indicating an error occurred.
     *
     * @param exception The exception that caused the error
     * @param message Optional user-friendly message
     */
    data class Error(
        val exception: AppException,
        val message: String = exception.getUserMessage()
    ) : UiState<Nothing>

    /**
     * State indicating the content is empty.
     *
     * @param message Optional message explaining why content is empty
     */
    data class Empty(val message: String? = null) : UiState<Nothing>

    /**
     * Returns true if this state represents a loading state.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns true if this state represents a successful state.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this state represents an error state.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns the data if this is a Success state, null otherwise.
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Returns the exception if this is an Error state, null otherwise.
     */
    fun exceptionOrNull(): AppException? = (this as? Error)?.exception
}

/**
 * Converts this UiState to a loading state while preserving any existing data.
 * Useful for refresh operations where you want to show a loading indicator
 * while keeping stale data visible.
 */
data class RefreshableUiState<T>(
    val data: T? = null,
    val isRefreshing: Boolean = false,
    val error: AppException? = null
) {
    val hasData: Boolean
        get() = data != null

    val hasError: Boolean
        get() = error != null

    companion object {
        fun <T> idle(): RefreshableUiState<T> = RefreshableUiState()

        fun <T> loading(): RefreshableUiState<T> = RefreshableUiState(isRefreshing = true)

        fun <T> success(data: T): RefreshableUiState<T> = RefreshableUiState(data = data)

        fun <T> error(exception: AppException, data: T? = null): RefreshableUiState<T> =
            RefreshableUiState(data = data, error = exception)

        fun <T> refreshing(data: T?): RefreshableUiState<T> =
            RefreshableUiState(data = data, isRefreshing = true)
    }
}

/**
 * One-time UI events that should be consumed once (e.g., navigation, snackbar).
 */
sealed interface UiEvent {
    /**
     * Navigate to a destination.
     */
    data class Navigate(val route: String) : UiEvent

    /**
     * Navigate back.
     */
    data object NavigateBack : UiEvent

    /**
     * Show a snackbar message.
     */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent

    /**
     * Show a toast message.
     */
    data class ShowToast(val message: String) : UiEvent

    /**
     * Show a dialog.
     */
    data class ShowDialog(
        val title: String,
        val message: String,
        val confirmLabel: String = "OK",
        val dismissLabel: String? = null
    ) : UiEvent
}

/**
 * Snackbar duration options.
 */
enum class SnackbarDuration {
    Short,
    Long,
    Indefinite
}
