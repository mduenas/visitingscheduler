package com.markduenas.visischeduler.presentation.viewmodel

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.common.util.AppResult
import com.markduenas.visischeduler.presentation.state.UiEvent
import com.markduenas.visischeduler.presentation.state.UiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 *
 * Features:
 * - Coroutine scope with SupervisorJob for fault tolerance
 * - Common exception handling
 * - UI state management
 * - One-time event channel
 *
 * @param S The type of UI state managed by this ViewModel
 */
abstract class BaseViewModel<S : Any>(initialState: S) {

    private val job = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }

    /**
     * The coroutine scope used by this ViewModel.
     * Uses SupervisorJob to prevent child coroutine failures from canceling siblings.
     */
    protected val viewModelScope = CoroutineScope(Dispatchers.Main + job + exceptionHandler)

    /**
     * The mutable UI state.
     */
    protected val _uiState = MutableStateFlow(initialState)

    /**
     * The observable UI state.
     */
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    /**
     * Channel for one-time UI events.
     */
    private val _events = Channel<UiEvent>(Channel.BUFFERED)

    /**
     * Flow of one-time UI events.
     */
    val events = _events.receiveAsFlow()

    /**
     * The current state value.
     */
    protected val currentState: S
        get() = _uiState.value

    /**
     * Updates the UI state using a reducer function.
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }

    /**
     * Sends a one-time event to the UI.
     */
    protected fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _events.send(event)
        }
    }

    /**
     * Shows a snackbar with the given message.
     */
    protected fun showSnackbar(message: String, actionLabel: String? = null) {
        sendEvent(UiEvent.ShowSnackbar(message, actionLabel))
    }

    /**
     * Shows a toast with the given message.
     */
    protected fun showToast(message: String) {
        sendEvent(UiEvent.ShowToast(message))
    }

    /**
     * Navigates to the given route.
     */
    protected fun navigate(route: String) {
        sendEvent(UiEvent.Navigate(route))
    }

    /**
     * Navigates back.
     */
    protected fun navigateBack() {
        sendEvent(UiEvent.NavigateBack)
    }

    /**
     * Launches a coroutine that handles errors automatically.
     */
    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Executes a suspending operation and handles the result.
     */
    protected suspend fun <T> executeWithResult(
        onLoading: () -> Unit = {},
        onSuccess: (T) -> Unit = {},
        onError: (AppException) -> Unit = {},
        block: suspend () -> AppResult<T>
    ) {
        onLoading()
        when (val result = block()) {
            is AppResult.Success -> onSuccess(result.data)
            is AppResult.Error -> {
                handleError(result.exception)
                onError(result.exception)
            }
            is AppResult.Loading -> onLoading()
        }
    }

    /**
     * Maps an AppResult to a UiState.
     */
    protected fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
        is AppResult.Success -> UiState.Success(data)
        is AppResult.Error -> UiState.Error(exception)
        is AppResult.Loading -> UiState.Loading
    }

    /**
     * Handles errors. Can be overridden by subclasses for custom error handling.
     */
    protected open fun handleError(throwable: Throwable) {
        val exception = when (throwable) {
            is AppException -> throwable
            else -> AppException.UnknownException(
                throwable.message ?: "An unexpected error occurred",
                throwable
            )
        }

        logError(exception)
        showErrorMessage(exception)
    }

    /**
     * Logs an error. Override to customize logging behavior.
     */
    protected open fun logError(exception: AppException) {
        // Default implementation - can be overridden for custom logging
        println("Error [${exception.getErrorCode()}]: ${exception.message}")
    }

    /**
     * Shows an error message to the user.
     */
    protected open fun showErrorMessage(exception: AppException) {
        showSnackbar(exception.getUserMessage())
    }

    /**
     * Called when the ViewModel is no longer needed.
     * Cancels all coroutines and cleans up resources.
     */
    open fun onCleared() {
        viewModelScope.cancel()
    }
}

/**
 * Simple state holder for ViewModels that don't need complex state management.
 */
data class SimpleViewModelState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: AppException? = null
) {
    val hasData: Boolean
        get() = data != null

    val hasError: Boolean
        get() = error != null

    companion object {
        fun <T> initial(): SimpleViewModelState<T> = SimpleViewModelState()
        fun <T> loading(): SimpleViewModelState<T> = SimpleViewModelState(isLoading = true)
        fun <T> success(data: T): SimpleViewModelState<T> = SimpleViewModelState(data = data)
        fun <T> error(exception: AppException): SimpleViewModelState<T> =
            SimpleViewModelState(error = exception)
    }
}
