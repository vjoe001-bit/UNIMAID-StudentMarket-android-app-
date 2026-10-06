package com.example.core.theme

/**
 * Visual theme modes supported across UNIMAID StudentMarket.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    val displayName: String
        get() = when (this) {
            LIGHT -> "Light Mode"
            DARK -> "Dark Mode"
            SYSTEM -> "System Default"
        }
}
