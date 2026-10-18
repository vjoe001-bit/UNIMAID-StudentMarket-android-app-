package com.example

import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.core.network.SupabaseResult
import com.example.core.realtime.SupabaseRealtimeManager
import com.example.data.models.CreateConversationDto
import com.example.data.models.CreateMessageDto
import com.example.data.models.CreateNotificationDto
import com.example.data.models.CreateOrderDto
import com.example.data.models.CreateProfileDto
import com.example.data.models.CreateVerificationRequestDto
import com.example.data.models.Order
import com.example.data.models.OrderItem
import com.example.data.models.Profile
import com.example.data.repository.AdminRepository
import com.example.data.repository.AdminRepositoryImpl
import com.example.data.repository.OrderRepository
import com.example.data.repository.OrderRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuditAndSecurityHardeningTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun verify_CreateOrderDto_does_not_serialize_joined_relations_to_PostgREST() {
        val dto = CreateOrderDto(
            id = "order-123",
            buyerId = "buyer-123",
            sellerId = "seller-456",
            listingId = "listing-789",
            quantity = 2,
            totalAmount = 5000.0,
            status = "PENDING",
            meetingLocation = "Ramat Library",
            buyerNote = "Meet at 2pm"
        )
        val adapter = moshi.adapter(CreateOrderDto::class.java)
        val json = adapter.toJson(dto)

        // Must include valid DB columns
        assertTrue(json.contains("\"buyer_id\":\"buyer-123\""))
        assertTrue(json.contains("\"seller_id\":\"seller-456\""))
        assertTrue(json.contains("\"listing_id\":\"listing-789\""))
        assertTrue(json.contains("\"total_amount\":5000.0"))

        // Must NOT serialize joined relations that trigger PGRST204 errors in PostgREST
        assertFalse(json.contains("\"buyer\":"))
        assertFalse(json.contains("\"seller\":"))
        assertFalse(json.contains("\"listing\":"))
        assertFalse(json.contains("\"items\":"))
        assertFalse(json.contains("\"history\":"))
    }

    @Test
    fun verify_CreateConversationDto_does_not_serialize_joined_profiles() {
        val dto = CreateConversationDto(
            id = "conv-123",
            buyerId = "student-1",
            sellerId = "student-2",
            listingId = "listing-1"
        )
        val adapter = moshi.adapter(CreateConversationDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"buyer_id\":\"student-1\""))
        assertTrue(json.contains("\"seller_id\":\"student-2\""))
        assertTrue(json.contains("\"listing_id\":\"listing-1\""))

        // Ensure joined profiles are excluded
        assertFalse(json.contains("\"buyer\":"))
        assertFalse(json.contains("\"seller\":"))
        assertFalse(json.contains("\"listing\":"))
        assertFalse(json.contains("\"otherParticipant\":"))
    }

    @Test
    fun verify_CreateMessageDto_does_not_serialize_sender_or_receiver_columns() {
        val dto = CreateMessageDto(
            id = "msg-123",
            conversationId = "conv-123",
            senderId = "sender-123",
            message = "Is this still available?",
            isRead = false
        )
        val adapter = moshi.adapter(CreateMessageDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"conversation_id\":\"conv-123\""))
        assertTrue(json.contains("\"sender_id\":\"sender-123\""))
        assertTrue(json.contains("\"message\":\"Is this still available?\""))

        // In messages table, there is no receiver_id or sender profile column
        assertFalse(json.contains("\"receiver_id\""))
        assertFalse(json.contains("\"sender\":"))
    }

    @Test
    fun verify_CreateNotificationDto_does_not_serialize_nonexistent_columns() {
        val dto = CreateNotificationDto(
            id = "notif-123",
            userId = "user-123",
            title = "Order Accepted",
            message = "Seller accepted your request",
            type = "PURCHASE_ACCEPTED",
            isRead = false
        )
        val adapter = moshi.adapter(CreateNotificationDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"user_id\":\"user-123\""))
        assertTrue(json.contains("\"title\":\"Order Accepted\""))
        assertTrue(json.contains("\"message\":\"Seller accepted your request\""))

        // Body and data_payload should not be sent in base DTO to avoid schema cache 400s
        assertFalse(json.contains("\"body\":"))
        assertFalse(json.contains("\"data_payload\":"))
    }

    @Test
    fun verify_CreateProfileDto_only_serializes_valid_profile_columns() {
        val dto = CreateProfileDto(
            id = "user-123",
            fullName = "Amina Mohammed",
            matricNumber = "UG19/ENG/042",
            department = "Electrical Engineering",
            level = "400L",
            isVerified = false
        )
        val adapter = moshi.adapter(CreateProfileDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"full_name\":\"Amina Mohammed\""))
        assertTrue(json.contains("\"matric_number\":\"UG19/ENG/042\""))
        assertTrue(json.contains("\"department\":\"Electrical Engineering\""))

        // Never serialize email or phone_number in profile insert to avoid 400 schema error
        assertFalse(json.contains("\"email\":"))
        assertFalse(json.contains("\"phone\":"))
        assertFalse(json.contains("\"phone_number\":"))
        assertFalse(json.contains("\"is_suspended\":"))
        assertFalse(json.contains("\"can_sell\":"))
    }

    @Test
    fun verify_CreateVerificationRequestDto_serializes_user_id_and_matric_number() {
        val dto = CreateVerificationRequestDto(
            id = "verif-123",
            userId = "user-123",
            matricNumber = "UG20/MED/001",
            documentUrl = "https://example.com/idcard.jpg",
            status = "PENDING"
        )
        val adapter = moshi.adapter(CreateVerificationRequestDto::class.java)
        val json = adapter.toJson(dto)

        assertTrue(json.contains("\"user_id\":\"user-123\""))
        assertTrue(json.contains("\"matric_number\":\"UG20/MED/001\""))
        assertFalse(json.contains("\"student_id\":"))
        assertFalse(json.contains("\"student\":"))
    }

    @Test
    fun verify_OrderRepository_rejects_self_purchase() = runBlocking {
        val repository: OrderRepository = OrderRepositoryImpl()
        val selfOrder = Order(
            id = "ord-self",
            buyerId = "user-abc",
            sellerId = "user-abc", // same buyer and seller
            listingId = "list-1",
            totalAmount = 2500.0
        )
        val result = repository.createOrder(selfOrder, emptyList())
        assertTrue(result is SupabaseResult.Error)
        val errorMsg = (result as SupabaseResult.Error).userFriendlyMessage
        assertTrue(errorMsg.contains("own listing"))
    }

    @Test
    fun verify_OrderRepository_rejects_missing_item_id() = runBlocking {
        val repository: OrderRepository = OrderRepositoryImpl()
        val emptyOrder = Order(
            id = "ord-empty",
            buyerId = "buyer-1",
            sellerId = "seller-2",
            listingId = null,
            totalAmount = 1000.0
        )
        val result = repository.createOrder(emptyOrder, emptyList())
        assertTrue(result is SupabaseResult.Error)
        val errorMsg = (result as SupabaseResult.Error).userFriendlyMessage
        assertTrue(errorMsg.contains("No item specified"))
    }
}
