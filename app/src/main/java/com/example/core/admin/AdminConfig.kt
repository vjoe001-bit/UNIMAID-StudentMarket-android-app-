package com.example.core.admin

/**
 * Designated Campus Administrators for UNIMAID StudentMarket.
 * Ensures instant administrative privileges for designated platform leads.
 */
object AdminConfig {

    const val PRIMARY_ADMIN_UID = "fdb54fd6-7cf3-4a64-9b86-92cbe55a50bc"
    const val PRIMARY_ADMIN_EMAIL = "jd267904@gmail.com"

    val ADMIN_UIDS: Set<String> = setOf(
        PRIMARY_ADMIN_UID
    )

    val ADMIN_EMAILS: Set<String> = setOf(
        PRIMARY_ADMIN_EMAIL.lowercase()
    )

    fun isDesignatedAdmin(userId: String?, email: String?): Boolean {
        if (!userId.isNullOrBlank() && ADMIN_UIDS.contains(userId.trim())) {
            return true
        }
        if (!email.isNullOrBlank() && ADMIN_EMAILS.contains(email.trim().lowercase())) {
            return true
        }
        return false
    }
}
