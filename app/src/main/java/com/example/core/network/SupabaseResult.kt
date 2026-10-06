package com.example.core.network

/**
 * Result wrapper for Supabase database, auth, and network calls.
 */
sealed class SupabaseResult<out T> {
    data class Success<out T>(val data: T) : SupabaseResult<T>()
    
    data class Error(
        val userFriendlyMessage: String,
        val technicalDetails: String? = null,
        val errorCode: String? = null,
        val statusCode: Int? = null,
        val throwable: Throwable? = null
    ) : SupabaseResult<Nothing>()

    object Loading : SupabaseResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): SupabaseResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> Loading
        }
    }
}
