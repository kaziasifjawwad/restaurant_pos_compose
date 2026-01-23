package data.model

/**
 * A generic wrapper for API operation results.
 * Provides type-safe success/error handling.
 */
sealed class Result<out T> {
    /**
     * Represents a successful operation with data
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with error details
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()
    
    /**
     * Check if the result is successful
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Check if the result is an error
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Get data if successful, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Get data if successful, throw exception otherwise
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw cause ?: Exception(message)
    }
    
    /**
     * Map the success value to another type
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    /**
     * Execute action if successful
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Execute action if error
     */
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }
}

/**
 * Convert a suspend block to a Result
 */
suspend inline fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error", e)
    }
}
