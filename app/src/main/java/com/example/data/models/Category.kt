package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Category(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val icon: String? = null,
    @Json(name = "icon_name")
    val iconName: String? = null,
    val type: String? = "PRODUCT", // "PRODUCT" or "SERVICE"
    @Json(name = "is_active")
    val isActive: Boolean = true
) {
    val effectiveIcon: String
        get() = icon ?: iconName ?: "category"

    val effectiveSlug: String
        get() = slug ?: name.lowercase().replace(" ", "-").replace("&", "and")
}

object DefaultCategories {
    val LIST: List<Category> = listOf(
        Category(id = "3afa36c4-2e2c-461b-8b6d-9bc6d3185bf1", name = "Electronics", description = "Phones, laptops, accessories and electronic devices", icon = "devices"),
        Category(id = "3320d33d-78ee-461e-bb22-69ff2a03243a", name = "Fashion", description = "Clothing, shoes, bags and fashion items", icon = "checkroom"),
        Category(id = "685a22a7-cfdd-4713-8be6-a6ba228619c6", name = "Education", description = "Books, textbooks, study materials and academic items", icon = "menu_book"),
        Category(id = "d9826238-0bae-4dcb-961a-d66c470b0c1c", name = "Hostel & Room", description = "Hostel items, furniture and room essentials", icon = "home"),
        Category(id = "0bff9704-c693-4d74-b5cb-3e1a11f46f2c", name = "Food & Groceries", description = "Food, snacks, drinks and groceries", icon = "restaurant"),
        Category(id = "530b414c-b05c-47dc-b15a-cc68d12037af", name = "Beauty & Personal Care", description = "Beauty and personal care products", icon = "face"),
        Category(id = "431fc5a9-8bb1-4ae1-bec4-571a2419a15e", name = "Sports & Fitness", description = "Sports equipment and fitness items", icon = "fitness_center"),
        Category(id = "d7c88d47-3ccd-418e-859d-89bdcdfb7e2d", name = "Automobile", description = "Vehicle-related products and accessories", icon = "directions_car"),
        Category(id = "cd990056-3ac7-40d1-bc3a-d4dd74f40860", name = "Services", description = "Student and professional services", icon = "build"),
        Category(id = "a39482de-8523-444b-923f-62fea3a65138", name = "Others", description = "Other legitimate student marketplace items", icon = "category")
    )
}

