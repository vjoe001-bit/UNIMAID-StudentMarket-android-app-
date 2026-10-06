package com.example.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.ui.viewmodels.MarketplaceViewModel

data class CategoryDisplayItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val type: String
)

@Composable
fun CategoriesScreen(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    marketplaceViewModel: MarketplaceViewModel? = null
) {
    val uiState = marketplaceViewModel?.uiState?.collectAsState()?.value
    val dbCategories = uiState?.categories ?: emptyList()

    val fallbackCategories = listOf(
        CategoryDisplayItem("textbooks", "Textbooks & Course Packs", "Faculty course packs, lecture notes, reference books", Icons.Outlined.Book, "Product"),
        CategoryDisplayItem("hostel", "Hostel & Room Essentials", "Mattresses, fans, reading lamps, storage lockers", Icons.Outlined.Home, "Product"),
        CategoryDisplayItem("tech", "Laptops, Phones & Gadgets", "Laptops, chargers, power banks, scientific calculators", Icons.Outlined.Computer, "Product"),
        CategoryDisplayItem("phones", "Phones & Accessories", "Smartphones, earphones, cases, adapters", Icons.Outlined.PhoneAndroid, "Product"),
        CategoryDisplayItem("fashion", "Campus Fashion & Wears", "Bedsheets, shoes, bags, native and casual wear", Icons.Outlined.LocalMall, "Product"),
        CategoryDisplayItem("provisions", "Food & Provisions", "Cooking gas cylinders, food items, non-perishables", Icons.Outlined.Kitchen, "Product"),
        CategoryDisplayItem("academic", "Academic Materials", "Stationery, drawing boards, lab coats, calculators", Icons.Outlined.School, "Product"),
        CategoryDisplayItem("tutoring", "Tutoring & Academic Services", "Maths, GST, Engineering & Medical peer tutoring", Icons.Outlined.Handyman, "Service"),
        CategoryDisplayItem("repairs", "Tech & Phone Repairs", "Screen fixes, software installs, gadget repair", Icons.Outlined.Handyman, "Service"),
        CategoryDisplayItem("beauty", "Beauty & Personal Care", "Skincare, grooming, perfumes, hair care", Icons.Outlined.Spa, "Product"),
        CategoryDisplayItem("sports", "Sports & Fitness Gear", "Jerseys, footwear, gym equipment", Icons.Outlined.SportsSoccer, "Product")
    )

    fun mapIcon(slug: String, name: String): ImageVector {
        val lower = "$slug $name".lowercase()
        return when {
            lower.contains("textbook") || lower.contains("book") -> Icons.Outlined.Book
            lower.contains("laptop") || lower.contains("computer") || lower.contains("tech") -> Icons.Outlined.Computer
            lower.contains("phone") -> Icons.Outlined.PhoneAndroid
            lower.contains("hostel") || lower.contains("room") || lower.contains("furniture") -> Icons.Outlined.Home
            lower.contains("fashion") || lower.contains("cloth") || lower.contains("wear") -> Icons.Outlined.LocalMall
            lower.contains("food") || lower.contains("provision") -> Icons.Outlined.Kitchen
            lower.contains("service") || lower.contains("repair") -> Icons.Outlined.Handyman
            lower.contains("academic") -> Icons.Outlined.School
            lower.contains("beauty") -> Icons.Outlined.Spa
            lower.contains("sport") -> Icons.Outlined.SportsSoccer
            else -> Icons.Outlined.LocalMall
        }
    }

    val displayCategories = if (dbCategories.isNotEmpty()) {
        dbCategories.map { cat ->
            CategoryDisplayItem(
                id = cat.id,
                title = cat.name,
                description = cat.description ?: "Browse ${cat.name} available from UNIMAID students",
                icon = mapIcon(cat.effectiveSlug, cat.name),
                type = cat.type ?: "PRODUCT"
            )
        }
    } else {
        fallbackCategories
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        UnimaidTopAppBar(title = "Campus Categories")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = 96.dp, top = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
                Text(
                    text = "Explore University of Maiduguri Marketplace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(displayCategories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppRadius.lg)
                        .clickable { onCategoryClick(category.id) }
                        .testTag("category_card_${category.id}"),
                    shape = AppRadius.lg,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(AppRadius.md)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(AppSpacing.md))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
