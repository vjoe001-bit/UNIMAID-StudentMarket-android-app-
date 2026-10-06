package com.example.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * High-performance image compressor for UNIMAID StudentMarket.
 * Ensures crisp HD product images while keeping upload sizes under ~800KB.
 * Correctly accounts for camera EXIF orientation rotations.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    fun compressImageFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = MAX_DIMENSION,
        quality: Int = JPEG_QUALITY
    ): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds to calculate sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // Calculate inSampleSize
            var sampleSize = 1
            val maxEdge = max(originalWidth, originalHeight)
            while ((maxEdge / sampleSize) > (maxDimension * 1.5)) {
                sampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val rawBitmap: Bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return null

            // 3. Read EXIF orientation
            val rotationDegrees = getExifOrientationDegrees(context, uri)

            // 4. Scale down to target dimension if still exceeds maxDimension
            val currentMax = max(rawBitmap.width, rawBitmap.height)
            val matrix = Matrix()
            if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                matrix.postScale(scale, scale)
            }
            if (rotationDegrees != 0f) {
                matrix.postRotate(rotationDegrees)
            }

            val finalBitmap = if (!matrix.isIdentity) {
                val transformed = Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true
                )
                if (transformed != rawBitmap) {
                    rawBitmap.recycle()
                }
                transformed
            } else {
                rawBitmap
            }

            // 5. Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            finalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun getExifOrientationDegrees(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }
}
