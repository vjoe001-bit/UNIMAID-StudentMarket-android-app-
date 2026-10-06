package com.example.data.models

/**
 * Supported sorting criteria for real UNIMAID marketplace queries.
 * Maps directly to valid PostgREST order parameters.
 */
enum class MarketplaceSortOption(val label: String, val postgrestOrder: String) {
    NEWEST("Newest First", "created_at.desc"),
    PRICE_LOW_TO_HIGH("Price: Low to High", "price.asc"),
    PRICE_HIGH_TO_LOW("Price: High to Low", "price.desc"),
    OLDEST("Oldest First", "created_at.asc")
}

/**
 * Standard campus item conditions matching database schema constraints.
 */
enum class ItemCondition(val code: String, val label: String) {
    ALL("", "All"),
    NEW("NEW", "Brand New"),
    LIKE_NEW("LIKE_NEW", "Like New"),
    GOOD("GOOD", "Good"),
    FAIR("FAIR", "Fair"),
    USED("USED", "Used");

    companion object {
        fun format(code: String?): String = when (code?.uppercase()) {
            "NEW" -> "Brand New"
            "LIKE_NEW" -> "Like New"
            "GOOD" -> "Good Condition"
            "FAIR" -> "Fair Condition"
            "USED" -> "Used"
            null, "" -> "Good"
            else -> code
        }
    }
}
