package com.example.core.network.storage

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SupabaseStorageApi {

    @POST("object/{bucket}/{path}")
    suspend fun uploadObject(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body fileBody: RequestBody,
        @Header("x-upsert") upsert: String = "true",
        @Header("Content-Type") contentType: String
    ): Response<StorageUploadResponse>

    @POST("object/sign/{bucket}/{path}")
    suspend fun createSignedUrl(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: SignUrlRequest
    ): Response<SignUrlResponse>

    @DELETE("object/{bucket}/{path}")
    suspend fun deleteObject(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    @GET("object/authenticated/{bucket}/{path}")
    suspend fun downloadAuthenticatedObject(
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>
}
