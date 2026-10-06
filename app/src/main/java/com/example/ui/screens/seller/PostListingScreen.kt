package com.example.ui.screens.seller

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.designsystem.components.UnimaidButton
import com.example.core.designsystem.components.UnimaidQuantityStepper
import com.example.core.designsystem.components.UnimaidTextField
import com.example.core.designsystem.components.UnimaidTopAppBar
import com.example.core.network.SupabaseResult
import com.example.core.session.SessionState
import com.example.core.theme.AccentCoral
import com.example.core.theme.AppRadius
import com.example.core.theme.AppSpacing
import com.example.core.theme.UnimaidGold
import com.example.core.theme.UnimaidGoldContainer
import com.example.core.theme.UnimaidGoldOnContainer
import com.example.core.theme.VerifiedGreen
import com.example.core.theme.VerifiedGreenContainer
import com.example.core.theme.VerifiedGreenOnContainer
import com.example.core.theme.WarningAmber
import com.example.data.models.Category
import com.example.data.models.Listing
import com.example.data.models.ListingImage
import com.example.data.repository.AuthRepository
import com.example.ui.viewmodels.MarketplaceViewModel
import kotlinx.coroutines.launch

private val CAMPUS_PRESET_LOCATIONS = listOf(
    "UNIMAID Main Campus",
    "Senate Building",
    "Main University Library",
    "Faculty of Engineering",
    "Faculty of Science",
    "Faculty of Arts",
    "Faculty of Agriculture",
    "Faculty of Law",
    "Complex Lecture Theatre (CLT)",
    "Hostel A (Male)",
    "Hostel B (Female)",
    "Commercial Area / ATM Point"
)

private val ITEM_CONDITIONS = listOf(
    "NEW" to "Brand New",
    "LIKE_NEW" to "Like New",
    "GOOD" to "Good Condition",
    "FAIR" to "Fair / Functional",
    "USED" to "Campus Used"
)

private val STATUS_OPTIONS = listOf(
    "ACTIVE" to "Available (Active)",
    "UNAVAILABLE" to "Temporarily Hidden (Deactivated)",
    "SOLD" to "Marked as Sold"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostListingScreen(
    authRepository: AuthRepository,
    marketplaceViewModel: MarketplaceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    listingId: String? = null // When null -> Create mode, when set -> Edit mode
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by authRepository.sessionState.collectAsState()
    val currentUserId = (sessionState as? SessionState.Authenticated)?.session?.userId ?: ""

    val uiState by marketplaceViewModel.uiState.collectAsState()
    val isEditMode = !listingId.isNullOrBlank()

    // Form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var condition by remember { mutableStateOf("GOOD") }
    var quantity by remember { mutableIntStateOf(1) }
    var campusLocation by remember { mutableStateOf("UNIMAID Main Campus") }
    var status by remember { mutableStateOf("ACTIVE") }

    // Initial snapshot values for change tracking
    var initialTitle by remember { mutableStateOf("") }
    var initialDescription by remember { mutableStateOf("") }
    var initialPriceText by remember { mutableStateOf("") }
    var initialCategoryId by remember { mutableStateOf<String?>(null) }
    var initialCondition by remember { mutableStateOf("GOOD") }
    var initialQuantity by remember { mutableIntStateOf(1) }
    var initialLocation by remember { mutableStateOf("UNIMAID Main Campus") }
    var initialStatus by remember { mutableStateOf("ACTIVE") }
    var isInitialDataLoaded by remember { mutableStateOf(!isEditMode) }

    // Images
    val newSelectedUris = remember { mutableStateListOf<Uri>() }
    val existingImages = remember { mutableStateListOf<ListingImage>() }
    val removedImages = remember { mutableStateListOf<ListingImage>() }

    // Validation & Progress state
    var titleError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var progressStatusText by remember { mutableStateOf("") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    // Track unsaved changes
    val hasUnsavedChanges by remember {
        derivedStateOf {
            if (!isInitialDataLoaded) return@derivedStateOf false
            if (isEditMode) {
                title != initialTitle ||
                    description != initialDescription ||
                    priceText != initialPriceText ||
                    selectedCategoryId != initialCategoryId ||
                    condition != initialCondition ||
                    quantity != initialQuantity ||
                    campusLocation != initialLocation ||
                    status != initialStatus ||
                    newSelectedUris.isNotEmpty() ||
                    removedImages.isNotEmpty()
            } else {
                title.isNotBlank() ||
                    description.isNotBlank() ||
                    priceText.isNotBlank() ||
                    selectedCategoryId != null ||
                    newSelectedUris.isNotEmpty()
            }
        }
    }

    // Intercept back button if there are unsaved changes
    BackHandler(enabled = hasUnsavedChanges && !isSubmitting) {
        showDiscardConfirmation = true
    }

    // Photo Picker Launcher
    val multiplePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        uris.forEach { uri ->
            if (!newSelectedUris.contains(uri)) {
                newSelectedUris.add(uri)
            }
        }
    }

    // Ensure categories are loaded
    LaunchedEffect(Unit) {
        if (uiState.categories.isEmpty()) {
            marketplaceViewModel.loadCategories()
        }
    }

    // Load initial data if editing
    LaunchedEffect(listingId) {
        if (isEditMode && listingId != null) {
            when (val res = marketplaceViewModel.getListingDetails(listingId)) {
                is SupabaseResult.Success -> {
                    val l = res.data
                    title = l.displayTitle
                    description = l.description ?: ""
                    priceText = if (l.price > 0) l.price.toInt().toString() else ""
                    selectedCategoryId = l.categoryId
                    condition = l.condition
                    quantity = l.quantity
                    campusLocation = l.displayLocation
                    status = l.status
                    existingImages.clear()
                    existingImages.addAll(l.images)

                    // Record initial values
                    initialTitle = title
                    initialDescription = description
                    initialPriceText = priceText
                    initialCategoryId = selectedCategoryId
                    initialCondition = condition
                    initialQuantity = quantity
                    initialLocation = campusLocation
                    initialStatus = status
                    isInitialDataLoaded = true
                }
                is SupabaseResult.Error -> {
                    Toast.makeText(context, "Error loading listing: ${res.userFriendlyMessage}", Toast.LENGTH_LONG).show()
                    isInitialDataLoaded = true
                }
                else -> {}
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            UnimaidTopAppBar(
                title = if (isEditMode) "Edit Campus Listing" else "Sell / Post Item",
                onBackClick = {
                    if (hasUnsavedChanges && !isSubmitting) {
                        showDiscardConfirmation = true
                    } else {
                        onNavigateBack()
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !isSubmitting,
                            modifier = Modifier.testTag("delete_listing_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Listing",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                ) {
                    UnimaidButton(
                        text = if (isSubmitting) progressStatusText.ifBlank { "Processing..." }
                        else if (isEditMode) "Save Changes"
                        else "Publish to UNIMAID Market",
                        onClick = {
                            // Validation
                            var hasError = false
                            val cleanTitle = title.trim()
                            if (cleanTitle.length < 3) {
                                titleError = "Title must be at least 3 characters"
                                hasError = true
                            } else if (cleanTitle.length > 100) {
                                titleError = "Title must be 100 characters or fewer"
                                hasError = true
                            } else {
                                titleError = null
                            }

                            val priceParsed = priceText.trim().toDoubleOrNull()
                            if (priceParsed == null || priceParsed <= 0) {
                                priceError = "Enter a valid price greater than ₦0"
                                hasError = true
                            } else if (priceParsed > 10_000_000) {
                                priceError = "Price exceeds allowable campus limit"
                                hasError = true
                            } else {
                                priceError = null
                            }

                            if (selectedCategoryId.isNullOrBlank()) {
                                categoryError = "Please select a category"
                                hasError = true
                            } else {
                                categoryError = null
                            }

                            if (campusLocation.trim().isBlank()) {
                                locationError = "Campus handover location cannot be blank"
                                hasError = true
                            } else {
                                locationError = null
                            }

                            if (hasError) return@UnimaidButton

                            if (currentUserId.isBlank()) {
                                Toast.makeText(context, "Please sign in to publish listings", Toast.LENGTH_LONG).show()
                                return@UnimaidButton
                            }

                            isSubmitting = true
                            coroutineScope.launch {
                                if (isEditMode && listingId != null) {
                                    val result = marketplaceViewModel.updateProductListing(
                                        context = context,
                                        listingId = listingId,
                                        sellerId = currentUserId,
                                        title = cleanTitle,
                                        description = description,
                                        categoryId = selectedCategoryId,
                                        condition = condition,
                                        price = priceParsed!!,
                                        quantity = quantity,
                                        campusLocation = campusLocation,
                                        status = status,
                                        newImageUris = newSelectedUris.toList(),
                                        existingImagesToKeep = existingImages.toList(),
                                        removedImages = removedImages.toList(),
                                        onProgress = { progressStatusText = it }
                                    )
                                    isSubmitting = false
                                    when (result) {
                                        is SupabaseResult.Success -> {
                                            Toast.makeText(context, "Listing updated successfully!", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                        is SupabaseResult.Error -> {
                                            Toast.makeText(context, result.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                        }
                                        else -> {}
                                    }
                                } else {
                                    val result = marketplaceViewModel.createProductListing(
                                        context = context,
                                        sellerId = currentUserId,
                                        title = cleanTitle,
                                        description = description,
                                        categoryId = selectedCategoryId,
                                        condition = condition,
                                        price = priceParsed!!,
                                        quantity = quantity,
                                        campusLocation = campusLocation,
                                        imageUris = newSelectedUris.toList(),
                                        onProgress = { progressStatusText = it }
                                    )
                                    isSubmitting = false
                                    when (result) {
                                        is SupabaseResult.Success -> {
                                            Toast.makeText(context, "Item posted successfully!", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                        is SupabaseResult.Error -> {
                                            Toast.makeText(context, result.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_listing_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(AppSpacing.md)
        ) {
            // Campus Policy Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppRadius.md,
                colors = CardDefaults.cardColors(containerColor = UnimaidGoldContainer)
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        tint = UnimaidGoldOnContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                    Column {
                        Text(
                            text = "UNIMAID Campus Meetup Policy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = UnimaidGoldOnContainer
                        )
                        Text(
                            text = "Safe daylight handovers in open campus areas. Inspect items before exchanging physical cash or bank transfer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UnimaidGoldOnContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // 1. PRODUCT IMAGES SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val totalImages = existingImages.size + newSelectedUris.size
                Text(
                    text = "$totalImages / 6",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "The first photo is the main cover. Tap 'Make Cover' on any image to set it as primary.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Photo Button
                if (existingImages.size + newSelectedUris.size < 6) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(AppRadius.md)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = AppRadius.md
                            )
                            .clickable(enabled = !isSubmitting) {
                                multiplePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("add_image_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Product Photos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Existing Images (when in Edit Mode)
                existingImages.forEachIndexed { index, img ->
                    val isFirstCover = index == 0
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(AppRadius.md)
                            .border(
                                width = if (isFirstCover) 2.dp else 1.dp,
                                color = if (isFirstCover) UnimaidGold else MaterialTheme.colorScheme.outlineVariant,
                                shape = AppRadius.md
                            )
                    ) {
                        AsyncImage(
                            model = img.imageUrl,
                            contentDescription = "Listing image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Cover badge
                        if (isFirstCover) {
                            Surface(
                                color = UnimaidGold,
                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Cover",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // "Make Cover" button for non-cover images
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(bottomStart = 8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clickable(enabled = !isSubmitting) {
                                        val item = existingImages.removeAt(index)
                                        existingImages.add(0, item)
                                    }
                            ) {
                                Text(
                                    text = "Make Cover",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Remove photo button
                        IconButton(
                            onClick = {
                                existingImages.remove(img)
                                removedImages.add(img)
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Newly Selected Local Images
                newSelectedUris.forEachIndexed { index, uri ->
                    val isFirstOverall = existingImages.isEmpty() && index == 0
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(AppRadius.md)
                            .border(
                                width = if (isFirstOverall) 2.dp else 1.dp,
                                color = if (isFirstOverall) UnimaidGold else MaterialTheme.colorScheme.outlineVariant,
                                shape = AppRadius.md
                            )
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "New selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isFirstOverall) {
                            Surface(
                                color = UnimaidGold,
                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Cover",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(bottomStart = 8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clickable(enabled = !isSubmitting) {
                                        val item = newSelectedUris.removeAt(index)
                                        newSelectedUris.add(0, item)
                                    }
                            ) {
                                Text(
                                    text = "Make Cover",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { newSelectedUris.remove(uri) },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // 2. BASIC DETAILS
            Text(
                text = "Listing Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            UnimaidTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = "Product Title *",
                placeholder = "e.g. Calculus by Thomas 14th Edition, HP Charger",
                isError = titleError != null,
                errorMessage = titleError,
                testTag = "input_listing_title"
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Category Selection Dropdown
            val availableCategories = if (uiState.categories.isNotEmpty()) uiState.categories else com.example.data.models.DefaultCategories.LIST
            val selectedCategory = availableCategories.find { it.id == selectedCategoryId }
            Column {
                Text(
                    text = "Category *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { if (!isSubmitting) isCategoryDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !isSubmitting)
                            .clip(AppRadius.md)
                            .clickable(enabled = !isSubmitting) {
                                isCategoryDropdownExpanded = !isCategoryDropdownExpanded
                            }
                            .testTag("select_category_button"),
                        shape = AppRadius.md,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (categoryError != null) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                Text(
                                    text = selectedCategory?.name ?: "Select Category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedCategory != null) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        availableCategories.forEach { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!cat.description.isNullOrBlank()) {
                                            Text(
                                                text = cat.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else null,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryError = null
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                if (categoryError != null) {
                    Text(
                        text = categoryError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Price & Quantity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1.3f)) {
                    UnimaidTextField(
                        value = priceText,
                        onValueChange = {
                            if (it.all { ch -> ch.isDigit() }) {
                                priceText = it
                                if (priceError != null) priceError = null
                            }
                        },
                        label = "Price (₦) *",
                        placeholder = "e.g. 3500",
                        leadingIcon = {
                            Text(
                                text = "₦",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = priceError != null,
                        errorMessage = priceError,
                        testTag = "input_listing_price"
                    )
                }

                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    UnimaidQuantityStepper(
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        minQuantity = 1,
                        maxQuantity = 99
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Condition Selection
            Text(
                text = "Item Condition *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ITEM_CONDITIONS.forEach { (key, label) ->
                    val isSelected = condition == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { condition = key },
                        label = { Text(label) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Description
            UnimaidTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Provide details like edition, department, working status, accessories included...",
                singleLine = false,
                maxLines = 4,
                testTag = "input_listing_description"
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // 3. LOCATION & STATUS
            Text(
                text = "Campus Location & Handover",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select safe campus meetup point for buyer inspection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Campus preset chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CAMPUS_PRESET_LOCATIONS.take(6).forEach { loc ->
                    val isSelected = campusLocation == loc
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            campusLocation = loc
                            if (locationError != null) locationError = null
                        },
                        label = { Text(loc, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            UnimaidTextField(
                value = campusLocation,
                onValueChange = {
                    campusLocation = it
                    if (locationError != null) locationError = null
                },
                label = "Meetup Location / Landmark *",
                placeholder = "e.g. Faculty of Engineering Quad, Main Library Entrance",
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                },
                isError = locationError != null,
                errorMessage = locationError,
                testTag = "input_campus_location"
            )

            // Status Selector (when in Edit Mode)
            if (isEditMode) {
                Spacer(modifier = Modifier.height(AppSpacing.lg))
                Text(
                    text = "Listing Availability Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Controls whether other students can see and order this item in the marketplace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STATUS_OPTIONS.forEach { (stKey, stLabel) ->
                        val isSelected = status == stKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { status = stKey },
                            label = { Text(stLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (stKey == "ACTIVE") VerifiedGreenContainer
                                else if (stKey == "SOLD") UnimaidGoldContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = if (stKey == "ACTIVE") VerifiedGreenOnContainer
                                else if (stKey == "SOLD") UnimaidGoldOnContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xxl))
        }
    }

    // Discard Unsaved Changes Dialog
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber) },
            title = { Text("Discard Unsaved Changes?") },
            text = { Text("You have unsaved changes on this listing. If you discard now, your updates will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && listingId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Campus Listing?") },
            text = { Text("This will permanently remove this item from UNIMAID StudentMarket and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        coroutineScope.launch {
                            isSubmitting = true
                            when (val res = marketplaceViewModel.deleteProductListing(listingId, currentUserId)) {
                                is SupabaseResult.Success -> {
                                    Toast.makeText(context, "Listing deleted.", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                                is SupabaseResult.Error -> {
                                    Toast.makeText(context, res.userFriendlyMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {}
                            }
                            isSubmitting = false
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
