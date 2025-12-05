package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.model.*
import kotlinx.coroutines.delay
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.FoodItemEditViewModel

/**
 * Screen for creating or editing a food item
 */
@Composable
fun FoodItemEditScreen(
    foodItemId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val viewModel = remember { FoodItemEditViewModel() }
    var isContentVisible by remember { mutableStateOf(false) }

    // Initialize based on mode
    LaunchedEffect(foodItemId) {
        if (foodItemId != null) {
            viewModel.initForEdit(foodItemId)
        } else {
            viewModel.initForCreate()
        }
        delay(100)
        isContentVisible = true
    }

    // Show error/success messages
    viewModel.errorMessage?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            EditScreenHeader(
                isEditMode = viewModel.isEditMode,
                onBack = onNavigateBack
            )

            // Main content
            if (viewModel.isLoading || viewModel.isLoadingLookups) {
                LoadingContent()
            } else {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(vertical = 24.dp)
                    ) {
                        // Section 1: Basic Info
                        item {
                            BasicInfoSection(
                                foodName = viewModel.foodName,
                                onFoodNameChange = viewModel::updateFoodName,
                                nameError = viewModel.nameError,
                                itemNumber = viewModel.itemNumber,
                                onItemNumberChange = viewModel::updateItemNumber,
                                itemNumberError = viewModel.itemNumberError
                            )
                        }

                        // Section 2: Food Categories
                        item {
                            CategorySection(
                                availableCategories = viewModel.getAvailableCategories(),
                                selectedCategories = viewModel.selectedCategories,
                                onAddCategory = viewModel::addCategory,
                                onRemoveCategory = viewModel::removeCategory
                            )
                        }

                        // Section 3: Food Variant Builder
                        item {
                            VariantBuilderSection(
                                availableSizes = viewModel.getAvailableFoodSizes(),
                                currentSize = viewModel.currentFoodSize,
                                onSizeChange = viewModel::updateCurrentFoodSize,
                                currentPrice = viewModel.currentFoodPrice,
                                onPriceChange = viewModel::updateCurrentFoodPrice,
                                availableIngredients = viewModel.getAvailableIngredients(),
                                currentIngredient = viewModel.currentIngredient,
                                onIngredientChange = viewModel::updateCurrentIngredient,
                                currentAmount = viewModel.currentIngredientAmount,
                                onAmountChange = viewModel::updateCurrentIngredientAmount,
                                currentUnit = viewModel.currentUnitOfMeasurement,
                                onUnitChange = viewModel::updateCurrentUnitOfMeasurement,
                                currentIngredientAmounts = viewModel.currentIngredientAmounts,
                                onAddIngredient = viewModel::addIngredientToCurrentVariant,
                                onRemoveIngredient = viewModel::removeIngredientFromCurrentVariant,
                                canAddIngredient = viewModel.canAddIngredient(),
                                canAddVariant = viewModel.canAddVariant(),
                                onAddVariant = viewModel::addFoodVariant
                            )
                        }

                        // Section 4: Added Variants
                        item {
                            AddedVariantsSection(
                                variants = viewModel.foodPrices,
                                onRemoveVariant = viewModel::removeFoodVariant
                            )
                        }

                        // Section 5: Description
                        item {
                            DescriptionSection(
                                description = viewModel.description,
                                onDescriptionChange = viewModel::updateDescription
                            )
                        }

                        // Action Buttons
                        item {
                            ActionButtonsSection(
                                isEditMode = viewModel.isEditMode,
                                isSaving = viewModel.isSaving,
                                onSave = { viewModel.save(onSaveSuccess) },
                                onCancel = onNavigateBack
                            )
                        }

                        // Bottom spacing
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }

        // Error Message Toast
        viewModel.errorMessage?.let { error ->
            ErrorToast(
                message = error,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Success Message Toast
        viewModel.successMessage?.let { message ->
            SuccessToast(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun EditScreenHeader(
    isEditMode: Boolean,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Column {
                Text(
                    text = if (isEditMode) "Edit Food Item" else "Create Food Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEditMode) "Update item details" else "Add a new menu item",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Section 1: Basic Info ====================

@Composable
private fun BasicInfoSection(
    foodName: String,
    onFoodNameChange: (String) -> Unit,
    nameError: String?,
    itemNumber: String,
    onItemNumberChange: (String) -> Unit,
    itemNumberError: String?
) {
    FormSection(title = "Basic Information", icon = Icons.Outlined.Info) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Food Name
            OutlinedTextField(
                value = foodName,
                onValueChange = onFoodNameChange,
                label = { Text("Food Name *") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Serial Number
            OutlinedTextField(
                value = itemNumber,
                onValueChange = onItemNumberChange,
                label = { Text("Serial Number *") },
                isError = itemNumberError != null,
                supportingText = itemNumberError?.let { { Text(it) } },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

// ==================== Section 2: Categories ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySection(
    availableCategories: List<FoodCategory>,
    selectedCategories: List<FoodCategory>,
    onAddCategory: (FoodCategory) -> Unit,
    onRemoveCategory: (FoodCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }

    FormSection(title = "Food Categories", icon = Icons.Outlined.Category) {
        // Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(0.5f),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (availableCategories.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No categories available") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onAddCategory(category)
                                selectedCategory = null
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Categories List
        if (selectedCategories.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Please select at least one category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCategories.forEach { category ->
                    CategoryChip(
                        category = category,
                        onRemove = { onRemoveCategory(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: FoodCategory,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ==================== Section 3: Variant Builder ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantBuilderSection(
    availableSizes: List<FoodSize>,
    currentSize: FoodSize?,
    onSizeChange: (FoodSize?) -> Unit,
    currentPrice: String,
    onPriceChange: (String) -> Unit,
    availableIngredients: List<Ingredient>,
    currentIngredient: Ingredient?,
    onIngredientChange: (Ingredient?) -> Unit,
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    currentUnit: UnitOfMeasurement?,
    onUnitChange: (UnitOfMeasurement?) -> Unit,
    currentIngredientAmounts: List<IngredientAmountRequest>,
    onAddIngredient: () -> Unit,
    onRemoveIngredient: (Long) -> Unit,
    canAddIngredient: Boolean,
    canAddVariant: Boolean,
    onAddVariant: () -> Unit
) {
    FormSection(title = "Food Variant", icon = Icons.Outlined.RestaurantMenu) {
        // Row 1: Size and Price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Size Dropdown
            var sizeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = currentSize?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Size") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    availableSizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.name) },
                            onClick = {
                                onSizeChange(size)
                                sizeExpanded = false
                            }
                        )
                    }
                }
            }

            // Price Input
            OutlinedTextField(
                value = currentPrice,
                onValueChange = onPriceChange,
                label = { Text("Price (৳)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Ingredient, Amount, Unit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Ingredient Dropdown
            var ingredientExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = ingredientExpanded,
                onExpandedChange = { ingredientExpanded = it },
                modifier = Modifier.weight(2f)
            ) {
                OutlinedTextField(
                    value = currentIngredient?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ingredient") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingredientExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = ingredientExpanded,
                    onDismissRequest = { ingredientExpanded = false }
                ) {
                    availableIngredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.name) },
                            onClick = {
                                onIngredientChange(ingredient)
                                ingredientExpanded = false
                            }
                        )
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = currentAmount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Unit Dropdown
            var unitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.weight(1.5f)
            ) {
                OutlinedTextField(
                    value = currentUnit?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    UnitOfMeasurement.entries.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.name) },
                            onClick = {
                                onUnitChange(unit)
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            // Add Ingredient Button
            Button(
                onClick = onAddIngredient,
                enabled = canAddIngredient,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Ingredient"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        // Current Ingredients List
        if (currentIngredientAmounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    currentIngredientAmounts.forEach { ingredient ->
                        IngredientRow(
                            ingredient = ingredient,
                            onRemove = { onRemoveIngredient(ingredient.ingredientId) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Variant Button
        Button(
            onClick = onAddVariant,
            enabled = canAddVariant,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Food Variant")
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: IngredientAmountRequest,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ingredient.ingredientName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = "${ingredient.amount}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = ingredient.unitOfMeasurement.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ==================== Section 4: Added Variants ====================

@Composable
private fun AddedVariantsSection(
    variants: List<FoodPriceRequest>,
    onRemoveVariant: (FoodSize) -> Unit
) {
    FormSection(title = "Added Variants", icon = Icons.Outlined.List) {
        if (variants.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Please add at least one food variant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                variants.forEach { variant ->
                    VariantCard(
                        variant = variant,
                        onRemove = { onRemoveVariant(variant.foodSize) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantCard(
    variant: FoodPriceRequest,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = variant.foodSize.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "৳ ${variant.foodPrice}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Ingredients List (Expandable)
            AnimatedVisibility(visible = expanded) {
                if (variant.ingredientAmountRequest.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ingredient",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Unit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        variant.ingredientAmountRequest.forEach { ingredient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ingredient.ingredientName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "${ingredient.amount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = ingredient.unitOfMeasurement.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No ingredients added",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

// ==================== Section 5: Description ====================

@Composable
private fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    FormSection(title = "Description", icon = Icons.Outlined.Description) {
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 6
        )
    }
}

// ==================== Action Buttons ====================

@Composable
private fun ActionButtonsSection(
    isEditMode: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel Button
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Save Button
        Button(
            onClick = onSave,
            enabled = !isSaving,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Saving...")
            } else {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Save else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Update" else "Save")
            }
        }
    }
}

// ==================== Helper Components ====================

@Composable
private fun FormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun ErrorToast(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SuccessToast(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

