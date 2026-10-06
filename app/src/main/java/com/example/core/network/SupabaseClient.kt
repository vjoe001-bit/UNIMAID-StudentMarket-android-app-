package com.example.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Public Supabase HTTP and Retrofit Client provider.
 * Handles token attachment, public anon headers, and network resilience.
 */
object SupabaseClient {

    @Volatile
    private var currentAccessToken: String? = null

    fun setAccessToken(token: String?) {
        currentAccessToken = token
    }

    fun getAccessToken(): String? = currentAccessToken

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val tokenToUse = currentAccessToken?.takeIf { it.isNotBlank() } ?: SupabaseConfig.anonKey

        val requestBuilder = originalRequest.newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Authorization", "Bearer $tokenToUse")
            .header("Accept", "application/json")

        // Only add Content-Type if not already specified (e.g. for uploads)
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val authRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.authBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val restRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.restBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val storageRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.storageBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    inline fun <reified T> createAuthApi(): T = authRetrofit.create(T::class.java)
    inline fun <reified T> createRestApi(): T = restRetrofit.create(T::class.java)
    inline fun <reified T> createStorageApi(): T = storageRetrofit.create(T::class.java)
}
