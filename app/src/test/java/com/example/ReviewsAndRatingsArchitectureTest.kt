package com.example

import com.example.core.network.SupabaseResult
import com.example.data.models.Order
import com.example.data.models.Review
import com.example.data.repository.ReviewRepository
import com.example.data.repository.SellerRatingSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewsAndRatingsArchitectureTest {

    private val buyerId = "buyer-uuid-1111"
    private val sellerId = "seller-uuid-2222"
    private val orderId = "order-uuid-3333"
    private val listingId = "listing-uuid-4444"

    /**
     * Fake implementation simulating Supabase reviews table and RLS enforcement.
     */
    private class FakeReviewRepository : ReviewRepository {
        val orders = mutableListOf<Order>()
        val reviews = mutableListOf<Review>()
        val notifications = mutableListOf<String>()

        override suspend fun getReviewsForSeller(sellerId: String): SupabaseResult<List<Review>> {
            val list = reviews.filter { it.reviewedUserId == sellerId }
            return SupabaseResult.Success(list)
        }

        override suspend fun getReviewsForListing(listingId: String): SupabaseResult<List<Review>> {
            val list = reviews.filter { it.listingId == listingId }
            return SupabaseResult.Success(list)
        }

        override suspend fun getReviewForOrder(orderId: String): SupabaseResult<Review?> {
            val review = reviews.find { it.orderId == orderId }
            return SupabaseResult.Success(review)
        }

        override suspend fun calculateSellerRating(sellerId: String): SupabaseResult<SellerRatingSummary> {
            val list = reviews.filter { it.reviewedUserId == sellerId }
            if (list.isEmpty()) {
                return SupabaseResult.Success(SellerRatingSummary())
            }
            val count = list.size
            val sum = list.sumOf { it.rating.toDouble() }
            val avg = Math.round((sum / count) * 10.0) / 10.0
            val five = list.count { it.rating == 5 }
            val four = list.count { it.rating == 4 }
            val three = list.count { it.rating == 3 }
            val two = list.count { it.rating == 2 }
            val one = list.count { it.rating == 1 }
            return SupabaseResult.Success(
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

        override suspend fun submitReview(
            orderId: String,
            reviewerId: String,
            reviewedUserId: String,
            listingId: String?,
            rating: Int,
            comment: String?
        ): SupabaseResult<Review> {
            if (rating !in 1..5) {
                return SupabaseResult.Error("Please select a rating between 1 and 5 stars.")
            }
            if (reviewerId.isBlank() || reviewedUserId.isBlank()) {
                return SupabaseResult.Error("Invalid reviewer or seller identity.")
            }
            if (reviewerId == reviewedUserId) {
                return SupabaseResult.Error("You cannot submit a review for your own account.")
            }

            val order = orders.find { it.id == orderId }
                ?: return SupabaseResult.Error("The referenced transaction could not be found.")

            if (!order.status.equals("COMPLETED", ignoreCase = true)) {
                return SupabaseResult.Error("Reviews are only permitted for completed campus handovers.")
            }
            if (order.buyerId != reviewerId) {
                return SupabaseResult.Error("Only the verified buyer of this transaction can write a review.")
            }
            if (order.sellerId != reviewedUserId) {
                return SupabaseResult.Error("The seller does not match this transaction record.")
            }

            if (reviews.any { it.orderId == orderId }) {
                return SupabaseResult.Error("You have already submitted a review for this transaction.")
            }

            val created = Review(
                id = "review-${reviews.size + 1}",
                orderId = orderId,
                reviewerId = reviewerId,
                reviewedUserId = reviewedUserId,
                listingId = listingId ?: order.listingId,
                rating = rating,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() }
            )
            reviews.add(created)
            notifications.add("Notified $reviewedUserId: $rating stars")
            return SupabaseResult.Success(created)
        }

        override suspend fun reportReview(
            reporterId: String,
            reviewId: String,
            reportedUserId: String,
            reason: String,
            description: String?
        ): SupabaseResult<com.example.data.models.Report> {
            return SupabaseResult.Success(
                com.example.data.models.Report(
                    id = "report-1",
                    reporterId = reporterId,
                    reportedUserId = reportedUserId,
                    reason = reason,
                    description = description
                )
            )
        }
    }

    private val fakeRepository = FakeReviewRepository()

    @Test
    fun calculateSellerRating_calculates_correct_distribution_and_average() = runBlocking {
        fakeRepository.reviews.addAll(
            listOf(
                Review(id = "r1", orderId = "o1", reviewerId = "u1", reviewedUserId = sellerId, rating = 5),
                Review(id = "r2", orderId = "o2", reviewerId = "u2", reviewedUserId = sellerId, rating = 5),
                Review(id = "r3", orderId = "o3", reviewerId = "u3", reviewedUserId = sellerId, rating = 4),
                Review(id = "r4", orderId = "o4", reviewerId = "u4", reviewedUserId = sellerId, rating = 2)
            )
        )

        val result = fakeRepository.calculateSellerRating(sellerId)

        assertTrue(result is SupabaseResult.Success)
        val summary = (result as SupabaseResult.Success).data
        assertEquals(4, summary.totalReviews)
        assertEquals(2, summary.fiveStarCount)
        assertEquals(1, summary.fourStarCount)
        assertEquals(0, summary.threeStarCount)
        assertEquals(1, summary.twoStarCount)
        assertEquals(0, summary.oneStarCount)
        assertEquals(4.0, summary.averageRating, 0.01)
    }

    @Test
    fun calculateSellerRating_returns_zero_when_no_reviews_exist() = runBlocking {
        val result = fakeRepository.calculateSellerRating(sellerId)

        assertTrue(result is SupabaseResult.Success)
        val summary = (result as SupabaseResult.Success).data
        assertEquals(0, summary.totalReviews)
        assertEquals(0.0, summary.averageRating, 0.01)
    }

    @Test
    fun submitReview_rejects_invalid_ratings() = runBlocking {
        val resLow = fakeRepository.submitReview(orderId, buyerId, sellerId, listingId, 0, "Too low")
        assertTrue(resLow is SupabaseResult.Error)
        assertEquals("Please select a rating between 1 and 5 stars.", (resLow as SupabaseResult.Error).userFriendlyMessage)

        val resHigh = fakeRepository.submitReview(orderId, buyerId, sellerId, listingId, 6, "Too high")
        assertTrue(resHigh is SupabaseResult.Error)
        assertEquals("Please select a rating between 1 and 5 stars.", (resHigh as SupabaseResult.Error).userFriendlyMessage)
    }

    @Test
    fun submitReview_rejects_self_reviews() = runBlocking {
        val res = fakeRepository.submitReview(orderId, sellerId, sellerId, listingId, 5, "I am great")
        assertTrue(res is SupabaseResult.Error)
        assertEquals("You cannot submit a review for your own account.", (res as SupabaseResult.Error).userFriendlyMessage)
    }

    @Test
    fun submitReview_rejects_uncompleted_transactions() = runBlocking {
        fakeRepository.orders.add(
            Order(
                id = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                listingId = listingId,
                totalAmount = 5000.0,
                status = "PENDING"
            )
        )

        val res = fakeRepository.submitReview(orderId, buyerId, sellerId, listingId, 5, "Pending order review")
        assertTrue(res is SupabaseResult.Error)
        assertEquals("Reviews are only permitted for completed campus handovers.", (res as SupabaseResult.Error).userFriendlyMessage)
    }

    @Test
    fun submitReview_rejects_when_reviewer_is_not_the_buyer() = runBlocking {
        fakeRepository.orders.add(
            Order(
                id = orderId,
                buyerId = "another-buyer-id",
                sellerId = sellerId,
                listingId = listingId,
                totalAmount = 5000.0,
                status = "COMPLETED"
            )
        )

        val res = fakeRepository.submitReview(orderId, buyerId, sellerId, listingId, 5, "Not my order")
        assertTrue(res is SupabaseResult.Error)
        assertEquals("Only the verified buyer of this transaction can write a review.", (res as SupabaseResult.Error).userFriendlyMessage)
    }

    @Test
    fun submitReview_rejects_duplicate_reviews_for_same_order() = runBlocking {
        fakeRepository.orders.add(
            Order(
                id = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                listingId = listingId,
                totalAmount = 5000.0,
                status = "COMPLETED"
            )
        )
        fakeRepository.reviews.add(
            Review(
                id = "existing-r1",
                orderId = orderId,
                reviewerId = buyerId,
                reviewedUserId = sellerId,
                rating = 5
            )
        )

        val res = fakeRepository.submitReview(orderId, buyerId, sellerId, listingId, 5, "Second review")
        assertTrue(res is SupabaseResult.Error)
        assertEquals("You have already submitted a review for this transaction.", (res as SupabaseResult.Error).userFriendlyMessage)
    }

    @Test
    fun submitReview_successful_creates_review_and_notifies_seller() = runBlocking {
        fakeRepository.orders.add(
            Order(
                id = orderId,
                buyerId = buyerId,
                sellerId = sellerId,
                listingId = listingId,
                totalAmount = 5000.0,
                status = "COMPLETED"
            )
        )

        val res = fakeRepository.submitReview(
            orderId = orderId,
            reviewerId = buyerId,
            reviewedUserId = sellerId,
            listingId = listingId,
            rating = 5,
            comment = "  Smooth meetup at Ramat library!  "
        )

        assertTrue(res is SupabaseResult.Success)
        val review = (res as SupabaseResult.Success).data
        assertEquals("review-1", review.id)
        assertEquals(5, review.rating)
        assertEquals("Smooth meetup at Ramat library!", review.comment)

        assertEquals(1, fakeRepository.notifications.size)
        assertTrue(fakeRepository.notifications.first().contains(sellerId))
    }
}
