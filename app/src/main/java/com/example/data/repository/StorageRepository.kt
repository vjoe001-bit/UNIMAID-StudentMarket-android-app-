package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.storage.SignUrlRequest
import com.example.core.network.storage.SupabaseStorageApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Storage Repository for UNIMAID StudentMarket.
 * Supports public buckets (listing-images, service-images, avatars)
 * and strictly protects the private verification-documents bucket via signed URLs.
 */
interface StorageRepository {
    fun getPublicUrl(bucket: String, path: String): String
    suspend fun uploadFile(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): SupabaseResult<String>
    suspend fun createSignedUrl(
        bucket: String,
        path: String,
        expiresInSeconds: Int = 3600
    ): SupabaseResult<String>
    suspend fun deleteFile(bucket: String, path: String): SupabaseResult<Unit>

    // Dedicated helpers
    suspend fun uploadListingImage(listingId: String, fileName: String, bytes: ByteArray): SupabaseResult<String>
    suspend fun deleteListingImage(listingId: String, fileName: String): SupabaseResult<Unit>
    suspend fun deleteListingImageByUrl(imageUrl: String): SupabaseResult<Unit>
    suspend fun uploadServiceImage(serviceId: String, fileName: String, bytes: ByteArray): SupabaseResult<String>
    suspend fun uploadAvatar(userId: String, fileName: String, bytes: ByteArray): SupabaseResult<String>
    suspend fun uploadVerificationDocument(userId: String, fileName: String, bytes: ByteArray): SupabaseResult<String>
    suspend fun getVerificationDocumentSignedUrl(path: String, expiresInSeconds: Int = 3600): SupabaseResult<String>
}

class StorageRepositoryImpl(
    private val storageApi: SupabaseStorageApi = SupabaseClient.createStorageApi()
) : StorageRepository {

    override fun getPublicUrl(bucket: String, path: String): String {
        return SupabaseConfig.StorageBuckets.getPublicUrl(bucket, path)
    }

    override suspend fun uploadFile(
        bucket: String,
        path: String,
        bytes: ByteArray,
        mimeType: String
    ): SupabaseResult<String> {
        return try {
            val mediaType = mimeType.toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)
            val cleanPath = path.trimStart('/')

            val response = storageApi.uploadObject(
                bucket = bucket,
                path = cleanPath,
                fileBody = requestBody,
                upsert = "true",
                contentType = mimeType
            )

            if (response.isSuccessful) {
                // If it's a public bucket, return the public URL; if private, return the path
                if (bucket == SupabaseConfig.StorageBuckets.VERIFICATION_DOCUMENTS) {
                    SupabaseResult.Success(cleanPath)
                } else {
                    SupabaseResult.Success(getPublicUrl(bucket, cleanPath))
                }
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun createSignedUrl(
        bucket: String,
        path: String,
        expiresInSeconds: Int
    ): SupabaseResult<String> {
        return try {
            val cleanPath = path.trimStart('/')
            val response = storageApi.createSignedUrl(
                bucket = bucket,
                path = cleanPath,
                request = SignUrlRequest(expiresIn = expiresInSeconds)
            )

            if (response.isSuccessful && response.body() != null) {
                val rawSignedUrl = response.body()!!.signedUrl
                val fullUrl = if (rawSignedUrl.startsWith("http")) {
                    rawSignedUrl
                } else {
                    "${SupabaseConfig.storageBaseUrl.trimEnd('/')}$rawSignedUrl"
                }
                SupabaseResult.Success(fullUrl)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun deleteFile(bucket: String, path: String): SupabaseResult<Unit> {
        return try {
            val cleanPath = path.trimStart('/')
            val response = storageApi.deleteObject(bucket, cleanPath)
            if (response.isSuccessful) {
                SupabaseResult.Success(Unit)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun uploadListingImage(
        listingId: String,
        fileName: String,
        bytes: ByteArray
    ): SupabaseResult<String> {
        val path = "$listingId/$fileName"
        return uploadFile(SupabaseConfig.StorageBuckets.LISTING_IMAGES, path, bytes)
    }

    override suspend fun deleteListingImage(
        listingId: String,
        fileName: String
    ): SupabaseResult<Unit> {
        val path = "$listingId/$fileName"
        return deleteFile(SupabaseConfig.StorageBuckets.LISTING_IMAGES, path)
    }

    override suspend fun deleteListingImageByUrl(imageUrl: String): SupabaseResult<Unit> {
        // Extract relative path from public URL
        val prefix = "/object/public/${SupabaseConfig.StorageBuckets.LISTING_IMAGES}/"
        val path = if (imageUrl.contains(prefix)) {
            imageUrl.substringAfter(prefix)
        } else {
            imageUrl.substringAfterLast("/")
        }
        return deleteFile(SupabaseConfig.StorageBuckets.LISTING_IMAGES, path)
    }

    override suspend fun uploadServiceImage(
        serviceId: String,
        fileName: String,
        bytes: ByteArray
    ): SupabaseResult<String> {
        val path = "$serviceId/$fileName"
        return uploadFile(SupabaseConfig.StorageBuckets.SERVICE_IMAGES, path, bytes)
    }

    override suspend fun uploadAvatar(
        userId: String,
        fileName: String,
        bytes: ByteArray
    ): SupabaseResult<String> {
        val path = "$userId/$fileName"
        return uploadFile(SupabaseConfig.StorageBuckets.AVATARS, path, bytes)
    }

    override suspend fun uploadVerificationDocument(
        userId: String,
        fileName: String,
        bytes: ByteArray
    ): SupabaseResult<String> {
        val path = "$userId/$fileName"
        // Uploads strictly to private verification-documents bucket
        return uploadFile(SupabaseConfig.StorageBuckets.VERIFICATION_DOCUMENTS, path, bytes)
    }

    override suspend fun getVerificationDocumentSignedUrl(
        path: String,
        expiresInSeconds: Int
    ): SupabaseResult<String> {
        // Enforces signed temporary token URL for private student ID documents
        return createSignedUrl(SupabaseConfig.StorageBuckets.VERIFICATION_DOCUMENTS, path, expiresInSeconds)
    }
}
