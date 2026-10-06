package com.example.core.network

import android.util.Log
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized Error Handler for Supabase operations.
 * Protects users from raw database strings, never reveals secret credentials,
 * and yields helpful, actionable guidance for UNIMAID students.
 */
object SupabaseErrorHandler {

    private const val TAG = "SupabaseErrorHandler"

    fun handleException(throwable: Throwable): SupabaseResult.Error {
        Log.e(TAG, "Network or system error encountered", throwable)

        return when (throwable) {
            is UnknownHostException -> {
                SupabaseResult.Error(
                    userFriendlyMessage = "Unable to connect to the campus marketplace. Please check your internet connection or UNIMAID Wi-Fi.",
                    technicalDetails = throwable.message,
                    errorCode = "NO_INTERNET",
                    throwable = throwable
                )
            }
            is SocketTimeoutException -> {
                SupabaseResult.Error(
                    userFriendlyMessage = "The request timed out. The campus network may be slow. Please try again.",
                    technicalDetails = throwable.message,
                    errorCode = "TIMEOUT",
                    throwable = throwable
                )
            }
            is IOException -> {
                SupabaseResult.Error(
                    userFriendlyMessage = "Network communication error. Please try again in a moment.",
                    technicalDetails = throwable.message,
                    errorCode = "IO_ERROR",
                    throwable = throwable
                )
            }
            else -> {
                SupabaseResult.Error(
                    userFriendlyMessage = "An unexpected error occurred. Please try again.",
                    technicalDetails = throwable.localizedMessage,
                    errorCode = "UNKNOWN",
                    throwable = throwable
                )
            }
        }
    }

    fun parseHttpError(statusCode: Int, errorBody: ResponseBody?): SupabaseResult.Error {
        val rawBody = runCatching { errorBody?.string() }.getOrNull()
        Log.w(TAG, "HTTP $statusCode error body: $rawBody")

        var extractedMessage: String? = null
        var extractedCode: String? = null
        var extractedHint: String? = null

        if (!rawBody.isNullOrBlank()) {
            runCatching {
                val json = JSONObject(rawBody)
                extractedMessage = json.optString("message").takeIf { it.isNotBlank() }
                    ?: json.optString("msg").takeIf { it.isNotBlank() }
                    ?: json.optString("error_description").takeIf { it.isNotBlank() }
                    ?: json.optString("error").takeIf { it.isNotBlank() }
                extractedCode = json.optString("error_code").takeIf { it.isNotBlank() }
                    ?: json.optString("code").takeIf { it.isNotBlank() }
                extractedHint = json.optString("hint").takeIf { it.isNotBlank() }
            }
        }

        val friendlyMessage = mapStatusAndCodeToMessage(statusCode, extractedCode, extractedMessage)

        return SupabaseResult.Error(
            userFriendlyMessage = friendlyMessage,
            technicalDetails = "HTTP $statusCode: $extractedMessage (Hint: $extractedHint)",
            errorCode = extractedCode ?: statusCode.toString(),
            statusCode = statusCode
        )
    }

    private fun mapStatusAndCodeToMessage(
        statusCode: Int,
        code: String?,
        serverMessage: String?
    ): String {
        val lowerMsg = serverMessage?.lowercase().orEmpty()

        // Auth errors
        if (lowerMsg.contains("invalid login credentials") || lowerMsg.contains("invalid grant")) {
            return "Incorrect email or password. Please verify your credentials and try again."
        }
        if (lowerMsg.contains("user already registered") || lowerMsg.contains("already exists")) {
            return "An account with this email address already exists. Try logging in instead."
        }
        if (lowerMsg.contains("email not confirmed")) {
            return "Please confirm your email address before logging in."
        }
        if (lowerMsg.contains("password should be at least")) {
            return "Password must be at least 6 characters long."
        }
        if (lowerMsg.contains("rate limit")) {
            return "Too many attempts. Please wait a few minutes before trying again."
        }

        // Database / RLS / PostgREST errors
        if (code == "42501" || statusCode == 403) {
            return "You do not have permission to perform this action under UNIMAID marketplace rules."
        }
        if (code == "23505") { // Unique violation
            return "This item or student record already exists in the system."
        }
        if (code == "PGRST116" || statusCode == 404) {
            return "The requested listing or profile was not found."
        }

        return when (statusCode) {
            400 -> "Invalid request. Please check the entered information."
            401 -> "Your session has expired. Please sign in again."
            403 -> "Access restricted. You do not have permission for this campus resource."
            404 -> "Item not found."
            409 -> "Conflict with existing marketplace data."
            429 -> "Too many requests. Please slow down and try again shortly."
            in 500..599 -> "The campus marketplace server is temporarily unavailable. Please try again soon."
            else -> "Something went wrong. Please try again."
        }
    }
}
