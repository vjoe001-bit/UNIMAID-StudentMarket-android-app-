package com.example.core.network.rest

import com.example.data.models.AdminRole
import com.example.data.models.CampusService
import com.example.data.models.CartItem
import com.example.data.models.Category
import com.example.data.models.Conversation
import com.example.data.models.CreateListingDto
import com.example.data.models.CreateListingImageDto
import com.example.data.models.Favorite
import com.example.data.models.Listing
import com.example.data.models.ListingImage
import com.example.data.models.Message
import com.example.data.models.Notification
import com.example.data.models.Order
import com.example.data.models.OrderItem
import com.example.data.models.OrderStatusHistory
import com.example.data.models.Profile
import com.example.data.models.Report
import com.example.data.models.Review
import com.example.data.models.UserSettings
import com.example.data.models.VerificationRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Supabase PostgREST endpoints for UNIMAID StudentMarket tables.
 * Fully typed, covering all marketplace features under RLS policies.
 */
interface SupabaseRestApi {

    // PROFILES
    @GET("profiles")
    suspend fun getProfileById(
        @Query("id") idFilter: String, // e.g. "eq.<userId>"
        @Query("select") select: String = "*"
    ): Response<List<Profile>>

    @POST("profiles")
    suspend fun insertProfile(
        @Body profile: Profile,
        @Query("on_conflict") onConflict: String = "id",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<Profile>>

    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body profile: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Profile>>

    // CATEGORIES
    @GET("categories")
    suspend fun getCategories(
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "display_order.asc,name.asc",
        @Query("select") select: String = "*"
    ): Response<List<Category>>

    // LISTINGS
    @GET("listings")
    suspend fun getListings(
        @Query("select") select: String = "*,category:categories(*),images:listing_images(*),seller:profiles(*)",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 30,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<Listing>>

    @GET("listings")
    suspend fun getListingById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*,category:categories(*),images:listing_images(*),seller:profiles(*)"
    ): Response<List<Listing>>

    @POST("listings")
    suspend fun createListing(
        @Body listing: CreateListingDto,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Listing>>

    @PATCH("listings")
    suspend fun updateListing(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Listing>>

    @DELETE("listings")
    suspend fun deleteListing(
        @Query("id") idFilter: String
    ): Response<Unit>

    // LISTING IMAGES
    @POST("listing_images")
    suspend fun insertListingImages(
        @Body images: List<CreateListingImageDto>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<ListingImage>>

    @DELETE("listing_images")
    suspend fun deleteListingImage(
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("listing_images")
    suspend fun deleteListingImagesByListing(
        @Query("listing_id") listingIdFilter: String
    ): Response<Unit>

    @PATCH("listing_images")
    suspend fun updateListingImage(
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<ListingImage>>

    // SERVICES
    @GET("services")
    suspend fun getServices(
        @Query("select") select: String = "*,images:service_images(*)",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 20,
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<CampusService>>

    // VERIFICATION REQUESTS
    @GET("verification_requests")
    suspend fun getVerificationRequests(
        @Query("select") select: String = "*,student:profiles!user_id(*)",
        @Query("order") order: String = "created_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<VerificationRequest>>

    @POST("verification_requests")
    suspend fun submitVerificationRequest(
        @Body request: VerificationRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<VerificationRequest>>

    @PATCH("verification_requests")
    suspend fun updateVerificationRequest(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<VerificationRequest>>

    // CART ITEMS
    @GET("cart_items")
    suspend fun getCartItems(
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*,listing:listings(*,images:listing_images(*),seller:profiles(*))",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<CartItem>>

    @GET("cart_items")
    suspend fun getCartItemForListing(
        @Query("user_id") userIdFilter: String,
        @Query("listing_id") listingIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<CartItem>>

    @POST("cart_items")
    suspend fun addCartItem(
        @Body item: CartItem,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<CartItem>>

    @PATCH("cart_items")
    suspend fun updateCartItemQuantity(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<CartItem>>

    @DELETE("cart_items")
    suspend fun deleteCartItem(
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("cart_items")
    suspend fun clearCartForUser(
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    // ORDERS & ORDER ITEMS
    @GET("orders")
    suspend fun getOrders(
        @Query("select") select: String = "*,buyer:profiles!buyer_id(*),seller:profiles!seller_id(*),listing:listings(*,images:listing_images(*)),history:order_status_history(*)",
        @Query("order") order: String = "created_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<Order>>

    @GET("orders")
    suspend fun getOrderById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*,buyer:profiles!buyer_id(*),seller:profiles!seller_id(*),listing:listings(*,images:listing_images(*)),history:order_status_history(*)"
    ): Response<List<Order>>

    @POST("orders")
    suspend fun createOrder(
        @Body order: Order,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Order>>

    @PATCH("orders")
    suspend fun updateOrder(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Order>>

    @POST("order_items")
    suspend fun insertOrderItems(
        @Body items: List<OrderItem>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<OrderItem>>

    @POST("order_status_history")
    suspend fun addOrderStatusHistory(
        @Body history: OrderStatusHistory,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<OrderStatusHistory>>

    // CONVERSATIONS & MESSAGES
    @GET("conversations")
    suspend fun getConversations(
        @Query("select") select: String = "*,buyer:profiles!buyer_id(*),seller:profiles!seller_id(*),listing:listings(*,images:listing_images(*))",
        @Query("order") order: String = "updated_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<Conversation>>

    @POST("conversations")
    suspend fun createConversation(
        @Body conversation: Conversation,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Conversation>>

    @PATCH("conversations")
    suspend fun updateConversation(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Conversation>>

    @GET("messages")
    suspend fun getMessages(
        @Query("conversation_id") conversationFilter: String,
        @Query("order") order: String = "created_at.asc",
        @Query("select") select: String = "*,sender:profiles!sender_id(*)"
    ): Response<List<Message>>

    @POST("messages")
    suspend fun sendMessage(
        @Body message: Message,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Message>>

    @PATCH("messages")
    suspend fun markMessageAsRead(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?> = mapOf("is_read" to true),
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Message>>

    // NOTIFICATIONS
    @GET("notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<Notification>>

    @PATCH("notifications")
    suspend fun updateNotification(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Notification>>

    @POST("notifications")
    suspend fun createNotification(
        @Body notification: Notification,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Notification>>

    // FAVORITES
    @GET("favorites")
    suspend fun getFavorites(
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*,listing:listings(*,images:listing_images(*),seller:profiles(*))",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<Favorite>>

    @POST("favorites")
    suspend fun addFavorite(
        @Body favorite: Favorite,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Favorite>>

    @DELETE("favorites")
    suspend fun deleteFavorite(
        @Query("user_id") userIdFilter: String,
        @Query("listing_id") listingIdFilter: String
    ): Response<Unit>

    // REVIEWS
    @GET("reviews")
    suspend fun getReviews(
        @Query("target_user_id") targetUserFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Review>>

    // REPORTS
    @POST("reports")
    suspend fun submitReport(
        @Body report: Report,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Report>>

    @GET("reports")
    suspend fun getReports(
        @Query("select") select: String = "*,reporter:profiles!reporter_id(*),reported_user:profiles!reported_user_id(*),listing:listings(*)",
        @Query("order") order: String = "created_at.desc",
        @QueryMap filters: Map<String, String> = emptyMap()
    ): Response<List<Report>>

    @PATCH("reports")
    suspend fun updateReport(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Report>>

    // PROFILES ADMIN & DIRECT
    @GET("profiles")
    suspend fun getAllProfiles(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<Profile>>

    @PATCH("profiles")
    suspend fun updateProfileAdmin(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Profile>>

    // USER SETTINGS
    @GET("user_settings")
    suspend fun getUserSettings(
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<UserSettings>>

    @POST("user_settings")
    suspend fun upsertUserSettings(
        @Body settings: UserSettings,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation"
    ): Response<List<UserSettings>>

    // ADMIN ROLES
    @GET("admin_roles")
    suspend fun getAdminRoleForUser(
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<AdminRole>>
}
