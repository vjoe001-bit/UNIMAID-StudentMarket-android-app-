package com.example.core.validation

import java.util.regex.Pattern

/**
 * UNIMAID StudentMarket Email Validation Rule.
 *
 * Normal email addresses (Gmail, Yahoo, Outlook, @unimaid.edu.ng, etc.) are allowed for authentication.
 * Email verification confirms the student's email address.
 * Student verification (matric number, student ID card, level, department) is handled separately.
 */
object StudentEmailValidator {

    /**
     * Official University of Maiduguri student email domains (recognized for academic identification).
     */
    val allowedDomains: Set<String> = linkedSetOf(
        "unimaid.edu.ng",
        "student.unimaid.edu.ng",
        "pg.unimaid.edu.ng"
    )

    /**
     * CONFIGURATION SWITCH:
     * False by default. Authentication allows any standard valid email provider (Gmail, Yahoo, etc.).
     * Student verification is handled separately through the student ID/matric verification flow.
     */
    var enforceDomainCheck: Boolean = false

    private val EMAIL_PATTERN: Pattern = Pattern.compile(
        "^[A-Z0-9._%+-]+@(?:[A-Z0-9-]+\\.)+[A-Z]{2,63}$",
        Pattern.CASE_INSENSITIVE
    )

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    /**
     * Validates an email address against syntax.
     * Normal Gmail, Yahoo, Outlook, and @unimaid.edu.ng addresses are all accepted.
     */
    fun validate(email: String): ValidationResult {
        val trimmed = email.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Please enter your email address.")
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.Invalid("Please enter a valid email address (e.g. user@gmail.com).")
        }

        if (enforceDomainCheck) {
            val domain = trimmed.substringAfter("@", "").lowercase()
            val matchesDomain = allowedDomains.any { allowed ->
                domain == allowed || domain.endsWith(".$allowed")
            }
            if (!matchesDomain) {
                val domainsList = allowedDomains.joinToString(", ")
                return ValidationResult.Invalid(
                    "Please use your official university email (ending in $domainsList)."
                )
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Checks if an email belongs to an official UNIMAID academic domain.
     * Note: Having an academic email is optional and does not replace student verification.
     */
    fun isAcademicDomain(email: String): Boolean {
        val domain = email.trim().substringAfter("@", "").lowercase()
        return allowedDomains.any { allowed ->
            domain == allowed || domain.endsWith(".$allowed")
        }
    }

    fun isValid(email: String): Boolean = validate(email) is ValidationResult.Valid
}
