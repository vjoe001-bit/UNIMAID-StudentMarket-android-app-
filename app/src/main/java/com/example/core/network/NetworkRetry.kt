package com.example.core.network

import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Executes a network call with exponential backoff retry.
 * Handles transient network dropouts common on campus Wi-Fi / mobile networks.
 */
suspend fun <T> withNetworkRetry(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> SupabaseResult<T>
): SupabaseResult<T> {
    var currentDelay = initialDelayMs
    var lastResult: SupabaseResult<T> = SupabaseResult.Error("Network error", errorCode = "INITIAL")

    for (attempt in 1..maxAttempts) {
        lastResult = block()
        if (lastResult is SupabaseResult.Success) {
            return lastResult
        }

        // Only retry on network/connection errors, not on 4xx client/validation errors
        if (lastResult is SupabaseResult.Error) {
            val isRetryable = lastResult.errorCode in listOf("NO_INTERNET", "TIMEOUT", "IO_ERROR") ||
                    (lastResult.statusCode != null && lastResult.statusCode in 500..599)
            if (!isRetryable || attempt == maxAttempts) {
                return lastResult
            }
        }

        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }
    return lastResult
}
