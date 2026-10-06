package com.example.core.network.auth

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAuthApi {

    @POST("signup")
    suspend fun signUp(
        @Body request: AuthSignUpRequest
    ): Response<AuthSessionResponse>

    @POST("token")
    suspend fun signInWithPassword(
        @Query("grant_type") grantType: String = "password",
        @Body request: AuthSignInRequest
    ): Response<AuthSessionResponse>

    @POST("token")
    suspend fun refreshToken(
        @Query("grant_type") grantType: String = "refresh_token",
        @Body request: AuthRefreshRequest
    ): Response<AuthSessionResponse>

    @POST("logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): Response<ResponseBody>

    @POST("recover")
    suspend fun recoverPassword(
        @Body request: AuthRecoverRequest
    ): Response<ResponseBody>

    @POST("resend")
    suspend fun resendVerificationEmail(
        @Body request: AuthResendRequest
    ): Response<ResponseBody>

    @GET("user")
    suspend fun getCurrentUser(
        @Header("Authorization") bearerToken: String
    ): Response<AuthUserData>
}
