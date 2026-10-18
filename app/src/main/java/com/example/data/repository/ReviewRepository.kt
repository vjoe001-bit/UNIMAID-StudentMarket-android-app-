package com.example.data.repository

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.SupabaseResult
import com.example.core.network.rest.SupabaseRestApi
import com.example.data.models.CreateReviewDto
import com.example.data.models.NotificationTypes
import com.example.data.models.Report
import com.example.data.models.Review
import java.util.UUID

/**
 * Summary calculations for reviews & ratings.
 */
data class SellerRatingSummary(
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val fiveStarCount: Int = 0,
    val fourStarCount: Int = 0,
    val threeStarCount: Int = 0,
    val twoStarCount: Int = 0,
    val oneStarCount: Int = 0
)

interface ReviewRepository {
    suspend fun getReviewsForSeller(sellerId: String): SupabaseResult<List<Review>>
    suspend fun getReviewsForListing(listingId: String): SupabaseResult<List<Review>>
    suspend fun getReviewForOrder(orderId: String): SupabaseResult<Review?>
    suspend fun calculateSellerRating(sellerId: String): SupabaseResult<SellerRatingSummary>
    suspend fun submitReview(
        orderId: String,
        reviewerId: String,
        reviewedUserId: String,
        listingId: String?,
        rating: Int,
        comment: String?
    ): SupabaseResult<Review>
    suspend fun reportReview(
        reporterId: String,
        reviewId: String,
        reportedUserId: String,
        reason: String,
        description: String?
    ): SupabaseResult<Report>
}

class ReviewRepositoryImpl(
    private val restApi: SupabaseRestApi = SupabaseClient.createRestApi(),
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(restApi),
    private val reportRepository: ReportRepository = ReportRepositoryImpl(restApi)
) : ReviewRepository {

    override suspend fun getReviewsForSeller(sellerId: String): SupabaseResult<List<Review>> {
        return try {
            val response = restApi.getReviewsForSeller("eq.$sellerId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getReviewsForListing(listingId: String): SupabaseResult<List<Review>> {
        return try {
            val response = restApi.getReviewsForListing("eq.$listingId")
            if (response.isSuccessful && response.body() != null) {
                SupabaseResult.Success(response.body()!!)
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun getReviewForOrder(orderId: String): SupabaseResult<Review?> {
        return try {
            val response = restApi.getReviewForOrder("eq.$orderId")
            if (response.isSuccessful) {
                val list = response.body()
                SupabaseResult.Success(list?.firstOrNull())
            } else {
                SupabaseErrorHandler.parseHttpError(response.code(), response.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun calculateSellerRating(sellerId: String): SupabaseResult<SellerRatingSummary> {
        return when (val reviewsRes = getReviewsForSeller(sellerId)) {
            is SupabaseResult.Success -> {
                val reviews = reviewsRes.data
                if (reviews.isEmpty()) {
                    SupabaseResult.Success(SellerRatingSummary())
                } else {
                    val count = reviews.size
                    val sum = reviews.sumOf { it.rating.toDouble() }
                    val avg = Math.round((sum / count) * 10.0) / 10.0
                    val five = reviews.count { it.rating == 5 }
                    val four = reviews.count { it.rating == 4 }
                    val three = reviews.count { it.rating == 3 }
                    val two = reviews.count { it.rating == 2 }
                    val one = reviews.count { it.rating == 1 }
                    SupabaseResult.Success(
                        SellerRatingSummary(
                            averageRating = avg,
                            totalReviews = count,
                            fiveStarCount = five,
                            fourStarCount = four,
                            threeStarCount = three,
                            twoStarCount = two,
                            oneStarCount = one
                        )
                    )
                }
            }
            is SupabaseResult.Error -> SupabaseResult.Error(reviewsRes.userFriendlyMessage)
            is SupabaseResult.Loading -> SupabaseResult.Loading
        }
    }

    override suspend fun submitReview(
        orderId: String,
        reviewerId: String,
        reviewedUserId: String,
        listingId: String?,
        rating: Int,
        comment: String?
    ): SupabaseResult<Review> {
        return try {
            // 1. Client-side sanity checks
            if (rating !in 1..5) {
                return SupabaseResult.Error("Please select a rating between 1 and 5 stars.")
            }
            if (reviewerId.isBlank() || reviewedUserId.isBlank()) {
                return SupabaseResult.Error("Invalid reviewer or seller identity.")
            }
            if (reviewerId == reviewedUserId) {
                return SupabaseResult.Error("You cannot submit a review for your own account.")
            }

            // 2. Verify order eligibility: must exist, be COMPLETED, reviewer is buyer, reviewed is seller
            val orderResp = restApi.getOrderById("eq.$orderId")
            if (!orderResp.isSuccessful || orderResp.body().isNullOrEmpty()) {
                return SupabaseResult.Error("The referenced transaction could not be found.")
            }
            val order = orderResp.body()!!.first()

            if (!order.status.equals("COMPLETED", ignoreCase = true)) {
                return SupabaseResult.Error("Reviews are only permitted for completed campus handovers.")
            }
            if (order.buyerId != reviewerId) {
                return SupabaseResult.Error("Only the verified buyer of this transaction can write a review.")
            }
            if (order.sellerId != reviewedUserId) {
                return SupabaseResult.Error("The seller does not match this transaction record.")
            }

            // 3. Check if order already has an existing review
            val existingReviewResp = restApi.getReviewForOrder("eq.$orderId")
            if (existingReviewResp.isSuccessful && !existingReviewResp.body().isNullOrEmpty()) {
                return SupabaseResult.Error("You have already submitted a review for this transaction.")
            }

            // Clean comment
            val sanitizedComment = comment?.trim()?.takeIf { it.isNotEmpty() }

            val effectiveListingId = listingId ?: order.listingId

            val dto = CreateReviewDto(
                orderId = orderId,
                reviewerId = reviewerId,
                reviewedUserId = reviewedUserId,
                listingId = effectiveListingId,
                rating = rating,
                comment = sanitizedComment
            )

            val insertResp = restApi.createReview(dto)
            if (insertResp.isSuccessful && !insertResp.body().isNullOrEmpty()) {
                val createdReview = insertResp.body()!!.first()

                // 4. Send notification to seller
                runCatching {
                    notificationRepository.createNotification(
                        userId = reviewedUserId,
                        title = "New Review Received",
                        message = "A student buyer rated your handover $rating/5 stars${if (!sanitizedComment.isNullOrBlank()) ": \"$sanitizedComment\"" else "."}",
                        type = NotificationTypes.REVIEW_RECEIVED,
                        dataPayload = mapOf(
                            "order_id" to orderId,
                            "review_id" to createdReview.id,
                            "seller_id" to reviewedUserId,
                            "entity_id" to createdReview.id,
                            "tab" to "sales"
                        )
                    )
                }

                SupabaseResult.Success(createdReview)
            } else {
                SupabaseErrorHandler.parseHttpError(insertResp.code(), insertResp.errorBody())
            }
        } catch (t: Throwable) {
            SupabaseErrorHandler.handleException(t)
        }
    }

    override suspend fun reportReview(
        reporterId: String,
        reviewId: String,
        reportedUserId: String,
        reason: String,
        description: String?
    ): SupabaseResult<Report> {
        val report = Report(
            id = UUID.randomUUID().toString(),
            reporterId = reporterId,
            reportedUserId = reportedUserId,
            reason = reason,
            description = "Report on Review $reviewId: ${description ?: "Inappropriate or abusive review content."}"
        )
        return reportRepository.submitReport(report)
    }
}
