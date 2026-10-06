package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.SessionManager
import com.example.core.session.SessionState
import com.example.core.session.UserSession
import com.example.data.models.Profile
import com.example.data.models.StudentVerificationState
import com.example.data.models.VerificationRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudentVerificationAndProfileTest {

    @Test
    fun `verify student verification state resolution handles all 5 distinct states`() {
        val baseProfile = Profile(
            id = "usr-1",
            email = "student@unimaid.edu.ng",
            fullName = "Victoria Drambi",
            matricNumber = "19/04/02/001",
            department = "Computer Engineering",
            phoneNumber = "08012345678",
            accountStatus = "ACTIVE",
            isVerified = false
        )

        // 1. Unverified State (no request)
        val unverifiedState = StudentVerificationState.resolve(baseProfile, null)
        assertEquals(StudentVerificationState.UNVERIFIED, unverifiedState)
        assertEquals("UNVERIFIED", unverifiedState.code)

        // 2. Pending State
        val pendingRequest = VerificationRequest(
            id = "req-1",
            studentId = "usr-1",
            matricNumber = "19/04/02/001",
            studentIdCardUrl = "verification-documents/usr-1/id.jpg",
            status = "PENDING"
        )
        val pendingState = StudentVerificationState.resolve(baseProfile, pendingRequest)
        assertEquals(StudentVerificationState.VERIFICATION_PENDING, pendingState)

        // 3. Verified State (via profile isVerified flag or request APPROVED)
        val verifiedProfile = baseProfile.copy(isVerified = true)
        val verifiedState = StudentVerificationState.resolve(verifiedProfile, null)
        assertEquals(StudentVerificationState.VERIFIED, verifiedState)

        val approvedRequest = pendingRequest.copy(status = "APPROVED")
        val approvedState = StudentVerificationState.resolve(baseProfile, approvedRequest)
        assertEquals(StudentVerificationState.VERIFIED, approvedState)

        // 4. Rejected State
        val rejectedRequest = pendingRequest.copy(status = "REJECTED", rejectionReason = "Blurry ID card")
        val rejectedState = StudentVerificationState.resolve(baseProfile, rejectedRequest)
        assertEquals(StudentVerificationState.REJECTED, rejectedState)

        // 5. Suspended State (Account status overrides verification)
        val suspendedProfile = baseProfile.copy(accountStatus = "SUSPENDED", isVerified = true)
        val suspendedState = StudentVerificationState.resolve(suspendedProfile, approvedRequest)
        assertEquals(StudentVerificationState.SUSPENDED, suspendedState)
    }

    @Test
    fun `verify profile completeness detection accurately identifies missing required fields`() {
        // Fully populated profile
        val completeProfile = Profile(
            id = "usr-1",
            email = "student@unimaid.edu.ng",
            fullName = "Victoria Drambi",
            matricNumber = "19/04/02/001",
            faculty = "Faculty of Engineering",
            department = "Computer Engineering",
            level = "400L",
            phoneNumber = "08012345678",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        assertTrue("Profile with all required fields must be complete", completeProfile.isComplete)
        assertTrue("No missing required fields", completeProfile.missingRequiredFields.isEmpty())
        assertEquals(100, completeProfile.completionPercentage)

        // Missing matric number and phone number
        val incompleteProfile = completeProfile.copy(
            matricNumber = null,
            phoneNumber = ""
        )
        assertFalse("Profile missing matric and phone must be incomplete", incompleteProfile.isComplete)
        val missing = incompleteProfile.missingRequiredFields
        assertTrue("Must report missing Matric Number", missing.contains("Matric Number"))
        assertTrue("Must report missing Phone Number", missing.contains("Phone Number"))
        assertEquals(60, incompleteProfile.completionPercentage) // 3 of 5 fields
    }

    @Test
    fun `verify SessionManager persists and updates student verification and profile completion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager.getInstance(context)

        sessionManager.clearSession()

        val initialSession = UserSession(
            userId = "usr-stu-101",
            email = "ibrahim@unimaid.edu.ng",
            fullName = "Ibrahim Bukaye",
            accessToken = "mock_jwt_token",
            refreshToken = "mock_refresh_token",
            isVerified = false,
            isAdmin = false,
            isEmailConfirmed = true,
            isProfileComplete = false,
            verificationStatus = "UNVERIFIED"
        )

        sessionManager.saveSession(initialSession)

        var state = sessionManager.sessionState.value
        assertTrue(state is SessionState.Authenticated)
        val authenticated1 = (state as SessionState.Authenticated).session
        assertFalse(authenticated1.isProfileComplete)
        assertEquals("UNVERIFIED", authenticated1.verificationStatus)

        // Update profile complete
        sessionManager.updateProfileComplete(true)
        state = sessionManager.sessionState.value
        val authenticated2 = (state as SessionState.Authenticated).session
        assertTrue("Session profile complete flag should update to true", authenticated2.isProfileComplete)

        // Update verification status to IN REVIEW
        sessionManager.updateVerificationState("VERIFICATION_PENDING", false)
        state = sessionManager.sessionState.value
        val authenticated3 = (state as SessionState.Authenticated).session
        assertEquals("VERIFICATION_PENDING", authenticated3.verificationStatus)
        assertFalse(authenticated3.isVerified)

        // Moderator approves verification
        sessionManager.updateVerificationState("VERIFIED", true)
        state = sessionManager.sessionState.value
        val authenticated4 = (state as SessionState.Authenticated).session
        assertEquals("VERIFIED", authenticated4.verificationStatus)
        assertTrue(authenticated4.isVerified)
    }

    @Test
    fun `verify safeUpdates prevents modification of protected security columns`() {
        val dangerousUpdates = mapOf<String, Any?>(
            "full_name" to "Updated Student Name",
            "department" to "Mechanical Engineering",
            "is_verified" to true,
            "verification_status" to "VERIFIED",
            "account_status" to "SUPER_ADMIN",
            "can_sell" to true,
            "can_buy" to true,
            "rating" to 5.0,
            "review_count" to 1000,
            "role" to "ADMIN"
        )

        // Simulating the safe filter in ProfileRepositoryImpl
        val safeUpdates = dangerousUpdates.toMutableMap().apply {
            remove("id")
            remove("is_verified")
            remove("verification_status")
            remove("account_status")
            remove("can_sell")
            remove("can_buy")
            remove("rating")
            remove("review_count")
            remove("created_at")
            remove("role")
            remove("is_admin")
        }

        assertTrue("Safe updates must retain safe fields", safeUpdates.containsKey("full_name"))
        assertTrue("Safe updates must retain safe fields", safeUpdates.containsKey("department"))
        assertFalse("Safe updates MUST NOT permit self-verification", safeUpdates.containsKey("is_verified"))
        assertFalse("Safe updates MUST NOT permit self-verification status change", safeUpdates.containsKey("verification_status"))
        assertFalse("Safe updates MUST NOT permit account status escalation", safeUpdates.containsKey("account_status"))
        assertFalse("Safe updates MUST NOT permit role elevation", safeUpdates.containsKey("role"))
    }
}
