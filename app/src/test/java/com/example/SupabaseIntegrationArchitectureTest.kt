package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.SupabaseClient
import com.example.core.network.SupabaseConfig
import com.example.core.network.SupabaseErrorHandler
import com.example.core.network.storage.SignUrlRequest
import com.example.core.realtime.SupabaseRealtimeManager
import com.example.core.session.SessionManager
import com.example.core.session.SessionState
import com.example.core.session.UserSession
import com.example.data.repository.StorageRepositoryImpl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseIntegrationArchitectureTest {

    @Test
    fun `verify supabase config endpoints and security rules`() {
        val url = SupabaseConfig.url
        assertTrue("Supabase URL must point to UNIMAID project", url.contains("uwgroqsvslyilomumial.supabase.co"))
        assertFalse("Must not be http", url.startsWith("http://"))

        val anonKey = SupabaseConfig.anonKey
        assertNotNull("Anon key must be configured", anonKey)
        assertTrue("Anon key must not be blank", anonKey.isNotBlank())
        assertFalse("Must never be service-role key", anonKey.contains("service_role"))

        assertEquals("https://uwgroqsvslyilomumial.supabase.co/auth/v1/", SupabaseConfig.authBaseUrl)
        assertEquals("https://uwgroqsvslyilomumial.supabase.co/rest/v1/", SupabaseConfig.restBaseUrl)
        assertEquals("https://uwgroqsvslyilomumial.supabase.co/storage/v1/", SupabaseConfig.storageBaseUrl)
        assertTrue("Realtime URL must start with wss", SupabaseConfig.realtimeWebSocketUrl.startsWith("wss://"))
    }

    @Test
    fun `verify supabase storage bucket declarations`() {
        assertEquals("listing-images", SupabaseConfig.StorageBuckets.LISTING_IMAGES)
        assertEquals("service-images", SupabaseConfig.StorageBuckets.SERVICE_IMAGES)
        assertEquals("avatars", SupabaseConfig.StorageBuckets.AVATARS)
        assertEquals("verification-documents", SupabaseConfig.StorageBuckets.VERIFICATION_DOCUMENTS)

        val publicUrl = SupabaseConfig.StorageBuckets.getPublicUrl("listing-images", "sample.jpg")
        assertEquals("https://uwgroqsvslyilomumial.supabase.co/storage/v1/object/public/listing-images/sample.jpg", publicUrl)
    }

    @Test
    fun `verify client initialization and dynamic token provider`() {
        assertNotNull(SupabaseClient.okHttpClient)
        assertNotNull(SupabaseClient.authRetrofit)
        assertNotNull(SupabaseClient.restRetrofit)
        assertNotNull(SupabaseClient.storageRetrofit)

        SupabaseClient.setAccessToken("test_student_jwt_token")
        assertEquals("test_student_jwt_token", SupabaseClient.getAccessToken())

        SupabaseClient.setAccessToken(null)
        assertEquals(null, SupabaseClient.getAccessToken())
    }

    @Test
    fun `verify centralized error handler translates network errors safely`() {
        val noInternetError = SupabaseErrorHandler.handleException(UnknownHostException("Unable to resolve host"))
        assertEquals("NO_INTERNET", noInternetError.errorCode)
        assertTrue(noInternetError.userFriendlyMessage.contains("Please check your internet connection"))

        val timeoutError = SupabaseErrorHandler.handleException(SocketTimeoutException("Read timed out"))
        assertEquals("TIMEOUT", timeoutError.errorCode)
        assertTrue(timeoutError.userFriendlyMessage.contains("timed out"))
    }

    @Test
    fun `verify centralized error handler translates rls and auth errors`() {
        // 403 / 42501 RLS error
        val rlsBody = "{\"code\": \"42501\", \"message\": \"new row violates row-level security policy\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val rlsResult = SupabaseErrorHandler.parseHttpError(403, rlsBody)
        assertTrue("Friendly message explains campus permission rules", rlsResult.userFriendlyMessage.contains("permission"))
        assertFalse("Must never expose raw secret details in user message", rlsResult.userFriendlyMessage.contains("row-level security"))

        // Invalid credentials error
        val authBody = "{\"error\": \"invalid_grant\", \"error_description\": \"Invalid login credentials\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val authResult = SupabaseErrorHandler.parseHttpError(400, authBody)
        assertTrue(authResult.userFriendlyMessage.contains("Incorrect email or password"))

        // Email not confirmed error
        val unconfirmedBody = "{\"code\": 400, \"error_code\": \"email_not_confirmed\", \"msg\": \"Email not confirmed\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val unconfirmedResult = SupabaseErrorHandler.parseHttpError(400, unconfirmedBody)
        assertTrue(unconfirmedResult.userFriendlyMessage.contains("confirm your email"))

        // Duplicate email registration error
        val duplicateBody = "{\"code\": 422, \"message\": \"User already registered\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val duplicateResult = SupabaseErrorHandler.parseHttpError(422, duplicateBody)
        assertTrue(duplicateResult.userFriendlyMessage.contains("already exists"))
    }

    @Test
    fun `verify session manager lifecycle and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager.getInstance(context)

        // Clear any previous state
        sessionManager.clearSession()
        assertTrue("Session must be unauthenticated after clear", sessionManager.sessionState.value is SessionState.Unauthenticated)

        // Save active session
        val mockSession = UserSession(
            userId = "student-12345",
            email = "student@unimaid.edu.ng",
            fullName = "Amina Bello",
            accessToken = "mock_jwt_access_token",
            refreshToken = "mock_refresh_token",
            isVerified = false,
            isAdmin = false
        )
        sessionManager.saveSession(mockSession)

        val stateAfterSave = sessionManager.sessionState.value
        assertTrue("State must be Authenticated", stateAfterSave is SessionState.Authenticated)
        val loaded = (stateAfterSave as SessionState.Authenticated).session
        assertEquals("student-12345", loaded.userId)
        assertEquals("student@unimaid.edu.ng", loaded.email)
        assertEquals("Amina Bello", loaded.fullName)
        assertFalse("New student must NOT be marked verified automatically", loaded.isVerified)

        // Update verification status explicitly when approved
        sessionManager.updateVerificationStatus(true)
        val updated = (sessionManager.sessionState.value as SessionState.Authenticated).session
        assertTrue("Student is verified after explicit admin approval", updated.isVerified)

        // Clean up
        sessionManager.clearSession()
        assertTrue(sessionManager.sessionState.value is SessionState.Unauthenticated)
    }

    @Test
    fun `verify profile model defaults and fallback creation safety`() {
        val testUserId = "550e8400-e29b-41d4-a716-446655440000"
        val profile = com.example.data.models.Profile(
            id = testUserId,
            fullName = "Ibrahim Danladi",
            email = "ibrahim@unimaid.edu.ng",
            matricNumber = "19/04/02/002",
            department = "Electrical Engineering",
            phoneNumber = "08012345678",
            level = "400L",
            isVerified = false,
            isSuspended = false
        )

        assertEquals(testUserId, profile.id)
        assertEquals("Ibrahim Danladi", profile.fullName)
        assertEquals("400L", profile.level)
        assertFalse(profile.isVerified)
        assertFalse(profile.isSuspended)
        assertTrue(profile.isComplete)
    }

    @Test
    fun `verify designated admin user has immediate admin role and permissions`() = kotlinx.coroutines.test.runTest {
        val adminUid = "fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc"
        val adminEmail = "jd267904@gmail.com"

        assertTrue(com.example.core.admin.AdminConfig.isDesignatedAdmin(adminUid, null))
        assertTrue(com.example.core.admin.AdminConfig.isDesignatedAdmin(null, adminEmail))
        assertTrue(com.example.core.admin.AdminConfig.isDesignatedAdmin(adminUid, adminEmail))
        assertFalse(com.example.core.admin.AdminConfig.isDesignatedAdmin("some-other-uuid", "student@gmail.com"))

        val adminRepo = com.example.data.repository.AdminRepositoryImpl()
        val roleResult = adminRepo.getAdminRole(adminUid)
        assertTrue(roleResult is com.example.core.network.SupabaseResult.Success)
        val role = (roleResult as com.example.core.network.SupabaseResult.Success).data
        assertNotNull(role)
        assertEquals("SUPER_ADMIN", role?.role)
        assertEquals(adminUid, role?.userId)
    }

    @Test
    fun `verify realtime manager initializes and provides typed event streams`() {
        val realtimeManager = SupabaseRealtimeManager(SupabaseClient.okHttpClient)
        assertNotNull(realtimeManager.messagesEvents)
        assertNotNull(realtimeManager.notificationsEvents)
        assertNotNull(realtimeManager.ordersEvents)
        assertNotNull(realtimeManager.conversationsEvents)
    }
}
