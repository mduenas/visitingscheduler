package com.markduenas.visischeduler.common.util

import com.markduenas.visischeduler.common.error.AppException

/**
 * A sealed class representing the result of an operation that can either succeed or fail.
 * This follows the Railway-Oriented Programming pattern for error handling.
 *
 * @param T The type of data returned on success
 */
sealed class AppResult<out T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : AppResult<T>()

    /**
     * Represents a failed operation with an exception.
     */
    data class Error(val exception: AppException) : AppResult<Nothing>()

    /**
     * Represents an operation in progress.
     */
    data object Loading : AppResult<Nothing>()

    /**
     * Returns true if this result represents a successful operation.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this result represents a failed operation.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this result represents a loading state.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if this is a Success, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the data if this is a Success, or throws the exception if it's an Error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    /**
     * Returns the exception if this is an Error, or null otherwise.
     */
    fun exceptionOrNull(): AppException? = when (this) {
        is Error -> exception
        else -> null
    }

    /**
     * Maps the success value to a new type.
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Flat maps the success value to a new Result.
     */
    inline fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> Loading
    }

    /**
     * Executes the given block if this is a Success.
     */
    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes the given block if this is an Error.
     */
    inline fun onError(action: (AppException) -> Unit): AppResult<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * Executes the given block if this is Loading.
     */
    inline fun onLoading(action: () -> Unit): AppResult<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * Folds this result into a single value.
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppException) -> R,
        onLoading: () -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
        is Loading -> onLoading()
    }

    companion object {
        /**
         * Creates a Success result with the given data.
         */
        fun <T> success(data: T): AppResult<T> = Success(data)

        /**
         * Creates an Error result with the given exception.
         */
        fun error(exception: AppException): AppResult<Nothing> = Error(exception)

        /**
         * Creates a Loading result.
         */
        fun loading(): AppResult<Nothing> = Loading

        /**
         * Wraps a suspending block in a try-catch and returns the appropriate Result.
         */
        suspend inline fun <T> runCatching(block: () -> T): AppResult<T> {
            return try {
                Success(block())
            } catch (e: AppException) {
                Error(e)
            } catch (e: Exception) {
                Error(AppException.UnknownException(e.message ?: "Unknown error", e))
            }
        }
    }
}

/**
 * Combines multiple Results into a single Result containing a list.
 */
fun <T> List<AppResult<T>>.combine(): AppResult<List<T>> {
    val errors = filterIsInstance<AppResult.Error>()
    if (errors.isNotEmpty()) {
        return errors.first()
    }

    val loading = filterIsInstance<AppResult.Loading>()
    if (loading.isNotEmpty()) {
        return AppResult.Loading
    }

    return AppResult.Success(
        filterIsInstance<AppResult.Success<T>>().map { it.data }
    )
}
