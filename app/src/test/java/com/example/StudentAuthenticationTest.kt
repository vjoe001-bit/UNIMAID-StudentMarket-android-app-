package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.SessionManager
import com.example.core.session.SessionState
import com.example.core.session.UserSession
import com.example.core.validation.StudentEmailValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StudentAuthenticationTest {

    @Test
    fun `verify student email validator allows normal Gmail and other standard email addresses by default`() {
        val normalAddresses = listOf(
            "joseph@gmail.com",
            "musa@gmail.com",
            "student123@gmail.com",
            "teststudent@gmail.com",
            "user@yahoo.com",
            "student@outlook.com"
        )

        for (email in normalAddresses) {
            val result = StudentEmailValidator.validate(email)
            assertTrue("Expected valid for $email", result is StudentEmailValidator.ValidationResult.Valid)
        }
    }

    @Test
    fun `verify student email validator allows valid UNIMAID academic addresses`() {
        val validAddresses = listOf(
            "student@unimaid.edu.ng",
            "fatima.aliyu@unimaid.edu.ng",
            "mustapha@student.unimaid.edu.ng",
            "postgrad@pg.unimaid.edu.ng",
            "ug_2019_042@unimaid.edu.ng"
        )

        for (email in validAddresses) {
            val result = StudentEmailValidator.validate(email)
            assertTrue("Expected valid for $email", result is StudentEmailValidator.ValidationResult.Valid)
        }
    }

    @Test
    fun `verify student email validator rejects non-UNIMAID addresses only when domain check is explicitly active`() {
        try {
            StudentEmailValidator.enforceDomainCheck = true

            val nonUnimaidAddresses = listOf(
                "student@gmail.com",
                "user@yahoo.com",
                "alumni@hotmail.com",
                "otheruni@unilag.edu.ng",
                "attacker@evil.com"
            )

            for (email in nonUnimaidAddresses) {
                val result = StudentEmailValidator.validate(email)
                assertTrue("Expected invalid for $email when domain check active", result is StudentEmailValidator.ValidationResult.Invalid)
            }
        } finally {
            StudentEmailValidator.enforceDomainCheck = false
        }
    }

    @Test
    fun `verify student email validator handles syntax errors gracefully`() {
        val malformed = listOf(
            "",
            "   ",
            "not-an-email",
            "@unimaid.edu.ng",
            "user@",
            "user@.edu.ng"
        )

        for (email in malformed) {
            val result = StudentEmailValidator.validate(email)
            assertTrue("Expected syntax failure for '$email'", result is StudentEmailValidator.ValidationResult.Invalid)
        }
    }

    @Test
    fun `verify student email validator configurable domain switch`() {
        try {
            StudentEmailValidator.enforceDomainCheck = true
            val invalidResult = StudentEmailValidator.validate("teststudent@gmail.com")
            assertTrue("When domain check is enabled, standard gmail fails", invalidResult is StudentEmailValidator.ValidationResult.Invalid)
        } finally {
            StudentEmailValidator.enforceDomainCheck = false
        }
    }

    @Test
    fun `verify session manager handles email confirmation and profile completion flags`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager.getInstance(context)

        sessionManager.clearSession()
        assertTrue(sessionManager.sessionState.value is SessionState.Unauthenticated)

        val session = UserSession(
            userId = "usr-test-99",
            email = "fatima@unimaid.edu.ng",
            fullName = "Fatima Ibrahim",
            accessToken = "token_abc_123",
            refreshToken = "refresh_def_456",
            isVerified = false,
            isAdmin = false,
            isEmailConfirmed = false,
            isProfileComplete = true
        )

        sessionManager.saveSession(session)
        val loaded = (sessionManager.sessionState.value as SessionState.Authenticated).session
        assertFalse("Email should initially be unconfirmed", loaded.isEmailConfirmed)
        assertTrue("Profile should be marked complete", loaded.isProfileComplete)

        // Confirm email
        sessionManager.updateEmailConfirmed(true)
        val afterConfirm = (sessionManager.sessionState.value as SessionState.Authenticated).session
        assertTrue("Email confirmed flag is updated", afterConfirm.isEmailConfirmed)

        sessionManager.clearSession()
        assertTrue(sessionManager.sessionState.value is SessionState.Unauthenticated)
    }
}
