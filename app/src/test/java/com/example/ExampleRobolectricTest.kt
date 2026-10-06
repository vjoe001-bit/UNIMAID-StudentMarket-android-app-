package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.branding.BrandConfig
import com.example.core.designsystem.components.MainNavTab
import com.example.core.network.SupabaseConfig
import com.example.core.theme.ThemeMode
import com.example.navigation.NavRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UNIMAID StudentMarket", appName)
    }

    @Test
    fun `verify brand constants and university identity`() {
        assertEquals("UNIMAID StudentMarket", BrandConfig.APP_NAME)
        assertEquals("UNIMAID Market", BrandConfig.APP_SHORT_NAME)
        assertEquals("University of Maiduguri", BrandConfig.UNIVERSITY_NAME)
        assertEquals("PrinceJoe.X", BrandConfig.CREATOR_ATTRIBUTION)
    }

    @Test
    fun `verify main bottom navigation tabs`() {
        val tabs = MainNavTab.values()
        assertEquals(5, tabs.size)
        assertTrue(tabs.contains(MainNavTab.HOME))
        assertTrue(tabs.contains(MainNavTab.MARKETPLACE))
        assertTrue(tabs.contains(MainNavTab.SELL_POST))
        assertTrue(tabs.contains(MainNavTab.MESSAGES))
        assertTrue(tabs.contains(MainNavTab.PROFILE))
    }

    @Test
    fun `verify navigation routes structure`() {
        assertEquals("welcome", NavRoutes.Welcome.route)
        assertEquals("login", NavRoutes.Login.route)
        assertEquals("signup", NavRoutes.SignUp.route)
        assertEquals("forgot_password", NavRoutes.ForgotPassword.route)
        assertEquals("student_verification", NavRoutes.StudentVerification.route)
        assertEquals("main_host", NavRoutes.MainHost.route)
        assertEquals("product_details/{listingId}", NavRoutes.ProductDetails.route)
        assertEquals("product_details/item-123", NavRoutes.ProductDetails.createRoute("item-123"))
    }

    @Test
    fun `verify campus in-person safety rules`() {
        assertTrue(BrandConfig.BUSINESS_POLICY_NO_ONLINE_PAYMENT.contains("in-person"))
        assertTrue(BrandConfig.PHYSICAL_MEETUP_NOTICE.contains("Senate Building"))
    }

    @Test
    fun `verify verification request model states`() {
        val pendingReq = com.example.data.models.VerificationRequest(
            id = "vr-1",
            studentId = "usr-1",
            matricNumber = "19/04/02/001",
            studentIdCardUrl = "verification-documents/usr-1/id.jpg",
            status = "PENDING"
        )
        assertEquals("PENDING", pendingReq.status)

        val approvedReq = pendingReq.copy(status = "APPROVED")
        assertEquals("APPROVED", approvedReq.status)

        val rejectedReq = pendingReq.copy(status = "REJECTED", rejectionReason = "Document unreadable")
        assertEquals("REJECTED", rejectedReq.status)
        assertEquals("Document unreadable", rejectedReq.rejectionReason)
    }

    @Test
    fun `verify theme modes and labels`() {
        assertEquals("Light Mode", ThemeMode.LIGHT.displayName)
        assertEquals("Dark Mode", ThemeMode.DARK.displayName)
        assertEquals("System Default", ThemeMode.SYSTEM.displayName)
    }

    @Test
    fun `verify supabase client configuration safety`() {
        assertNotNull(SupabaseConfig.url)
        assertTrue(SupabaseConfig.url.startsWith("https://"))
        assertNotNull(SupabaseConfig.anonKey)
        assertTrue(SupabaseConfig.anonKey.isNotBlank())
        assertEquals("profiles", SupabaseConfig.Tables.PROFILES)
        assertEquals("categories", SupabaseConfig.Tables.CATEGORIES)
        assertEquals("listings", SupabaseConfig.Tables.LISTINGS)
        assertEquals("listing_images", SupabaseConfig.Tables.LISTING_IMAGES)
        assertEquals("favorites", SupabaseConfig.Tables.FAVORITES)
        assertEquals("carts", SupabaseConfig.Tables.CARTS)
        assertEquals("cart_items", SupabaseConfig.Tables.CART_ITEMS)
        assertEquals("orders", SupabaseConfig.Tables.ORDERS)
        assertEquals("order_items", SupabaseConfig.Tables.ORDER_ITEMS)
        assertEquals("conversations", SupabaseConfig.Tables.CONVERSATIONS)
        assertEquals("messages", SupabaseConfig.Tables.MESSAGES)
        assertEquals("notifications", SupabaseConfig.Tables.NOTIFICATIONS)
        assertEquals("reports", SupabaseConfig.Tables.REPORTS)
        assertEquals("user_settings", SupabaseConfig.Tables.USER_SETTINGS)
    }

    @Test
    fun `verify listing model default values and campus location`() {
        val listing = com.example.data.models.Listing(
            id = "lst-101",
            sellerId = "usr-1",
            title = "Engineering Mathematics Textbook",
            price = 3500.0,
            condition = "LIKE_NEW"
        )
        assertEquals("GOOD", com.example.data.models.Listing(id = "1", sellerId = "u", title = "t", price = 10.0).condition)
        assertEquals("LIKE_NEW", listing.condition)
        assertEquals("UNIMAID Main Campus", listing.locationCampus)
        assertEquals(1, listing.quantity)
        assertEquals(false, listing.isSold)
        assertEquals(true, listing.isAvailable)
    }

    @Test
    fun `verify order model distinguishes transaction status from physical payment status`() {
        val order = com.example.data.models.Order(
            id = "ord-202",
            buyerId = "buyer-1",
            sellerId = "seller-1",
            totalAmount = 7000.0,
            status = "MEETING_ARRANGED",
            paymentStatus = "PHYSICAL_PENDING",
            meetupLocation = "UNIMAID Senate Building"
        )
        assertEquals("MEETING_ARRANGED", order.status)
        assertEquals("PHYSICAL_PENDING", order.paymentStatus)
        assertEquals("PHYSICAL_MEETUP", order.deliveryMethod)
        assertEquals("UNIMAID Senate Building", order.meetupLocation)
    }

    @Test
    fun `verify core marketplace repositories can be instantiated`() {
        val marketplaceRepo = com.example.data.repository.MarketplaceRepositoryImpl()
        val cartRepo = com.example.data.repository.CartRepositoryImpl()
        val orderRepo = com.example.data.repository.OrderRepositoryImpl()
        val favRepo = com.example.data.repository.FavoritesRepositoryImpl()
        val chatRepo = com.example.data.repository.ChatRepositoryImpl()
        val notifRepo = com.example.data.repository.NotificationRepositoryImpl()
        val settingsRepo = com.example.data.repository.UserSettingsRepositoryImpl()
        val profileRepo = com.example.data.repository.ProfileRepositoryImpl()
        assertNotNull(marketplaceRepo)
        assertNotNull(cartRepo)
        assertNotNull(orderRepo)
        assertNotNull(favRepo)
        assertNotNull(chatRepo)
        assertNotNull(notifRepo)
        assertNotNull(settingsRepo)
        assertNotNull(profileRepo)
    }

    @Test
    fun `verify profile model attributes and student fields`() {
        val profile = com.example.data.models.Profile(
            id = "std-1",
            email = "student@unimaid.edu.ng",
            fullName = "Fatima Mohammed",
            matricNumber = "19/04/02/089",
            department = "Computer Engineering",
            faculty = "Engineering",
            level = "300L",
            phoneNumber = "08012345678",
            bio = "Selling clean course handouts and hostel essentials.",
            isVerified = true,
            accountStatus = "ACTIVE"
        )
        assertEquals("Fatima Mohammed", profile.fullName)
        assertEquals("300L", profile.level)
        assertEquals("Computer Engineering", profile.department)
        assertEquals(true, profile.isVerified)
        assertEquals("ACTIVE", profile.accountStatus)
        assertEquals(true, profile.canSell)
        assertEquals(true, profile.canBuy)
    }

    @Test
    fun `verify session state lifecycle and restoration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = com.example.core.session.SessionManager.getInstance(context)
        sessionManager.clearSession()
        assertTrue(sessionManager.sessionState.value is com.example.core.session.SessionState.Unauthenticated)

        val testSession = com.example.core.session.UserSession(
            userId = "usr-test-1",
            email = "user@unimaid.edu.ng",
            fullName = "Ibrahim Musa",
            accessToken = "mock-access-token",
            refreshToken = "mock-refresh-token",
            isVerified = false,
            isAdmin = false
        )
        sessionManager.saveSession(testSession)
        val authState = sessionManager.sessionState.value
        assertTrue(authState is com.example.core.session.SessionState.Authenticated)
        assertEquals("usr-test-1", (authState as com.example.core.session.SessionState.Authenticated).session.userId)
        assertEquals("Ibrahim Musa", authState.session.fullName)

        sessionManager.updateVerificationStatus(true)
        val verifiedState = sessionManager.sessionState.value as com.example.core.session.SessionState.Authenticated
        assertTrue(verifiedState.session.isVerified)

        sessionManager.clearSession()
        assertTrue(sessionManager.sessionState.value is com.example.core.session.SessionState.Unauthenticated)
    }
}

