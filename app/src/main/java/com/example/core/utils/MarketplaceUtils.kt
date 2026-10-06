package com.example.core.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Utility helpers for the UNIMAID campus marketplace.
 */
object MarketplaceUtils {

    /**
     * Formats a monetary amount into standard Nigerian Naira display string (e.g. ₦18,500).
     */
    fun formatNaira(amount: Double?): String {
        if (amount == null || amount < 0) return "₦0"
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            "₦" + formatter.format(amount.toLong())
        } catch (e: Exception) {
            "₦${amount.toInt()}"
        }
    }

    /**
     * Formats ISO 8601 timestamp string into readable date (e.g. "Today", "Yesterday", "Oct 1").
     */
    fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Recently posted"
        return try {
            val datePart = isoString.substringBefore("T")
            "Posted $datePart"
        } catch (e: Exception) {
            "Recently posted"
        }
    }
}

/**
 * Convenient Double extension for Naira formatting.
 */
fun Double?.toNaira(): String = MarketplaceUtils.formatNaira(this)
