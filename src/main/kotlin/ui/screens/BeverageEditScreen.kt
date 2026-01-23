package ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.model.BeveragePrice
import data.model.QuantityUnit
import kotlinx.coroutines.delay
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.BeverageEditViewModel
import java.text.DecimalFormat

/**
 * Screen for creating or editing a beverage
 */
@Composable
fun BeverageEditScreen(
    beverageId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val viewModel = remember { BeverageEditViewModel() }
    var isContentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(beverageId) {
        if (beverageId != null) {
            viewModel.initForEdit(beverageId)
        } else {
            viewModel.initForCreate()
        }
        delay(100)
        isContentVisible = true
    }

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
            BeverageEditHeader(
                isEditMode = viewModel.isEditMode,
                onBack = onNavigateBack
            )

            if (viewModel.isLoading) {
                BeverageEditLoadingContent()
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
                            BeverageBasicInfoSection(
                                name = viewModel.beverageName,
                                onNameChange = viewModel::updateBeverageName,
                                nameError = viewModel.nameError
                            )
                        }

                        // Section 2: Price Builder
                        item {
                            BeveragePriceBuilderSection(
                                currentPrice = viewModel.currentPrice,
                                onPriceChange = viewModel::updateCurrentPrice,
                                currentQuantity = viewModel.currentQuantity,
                                onQuantityChange = viewModel::updateCurrentQuantity,
                                currentUnit = viewModel.currentUnit,
                                onUnitChange = viewModel::updateCurrentUnit,
                                canAddPrice = viewModel.canAddPrice(),
                                onAddPrice = viewModel::addBeveragePrice,
                                duplicateError = viewModel.duplicateError
                            )
                        }

                        // Section 3: Added Prices
                        item {
                            BeveragePricesListSection(
                                prices = viewModel.beveragePrices,
                                onRemovePrice = viewModel::removeBeveragePrice
                            )
                        }

                        // Action Buttons
                        item {
                            BeverageActionButtons(
                                isEditMode = viewModel.isEditMode,
                                isSaving = viewModel.isSaving,
                                onSave = { viewModel.save(onSaveSuccess) },
                                onCancel = onNavigateBack
                            )
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }

        // Error Toast
        viewModel.errorMessage?.let { error ->
            BeverageErrorToast(
                message = error,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Success Toast
        viewModel.successMessage?.let { message ->
            BeverageSuccessToast(
                message = message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BeverageEditHeader(
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (isEditMode) "Edit Beverage" else "Create Beverage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEditMode) "Update beverage details" else "Add a new drink",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BeverageEditLoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== Section 1: Basic Info ====================

@Composable
private fun BeverageBasicInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?
) {
    BeverageFormSection(title = "Beverage Information", icon = Icons.Outlined.LocalDrink) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Beverage Name *") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

// ==================== Section 2: Price Builder ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeveragePriceBuilderSection(
    currentPrice: String,
    onPriceChange: (String) -> Unit,
    currentQuantity: String,
    onQuantityChange: (String) -> Unit,
    currentUnit: QuantityUnit?,
    onUnitChange: (QuantityUnit?) -> Unit,
    canAddPrice: Boolean,
    onAddPrice: () -> Unit,
    duplicateError: String?
) {
    BeverageFormSection(title = "Add Price Option", icon = Icons.Outlined.PriceChange) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Price
            OutlinedTextField(
                value = currentPrice,
                onValueChange = onPriceChange,
                label = { Text("Price (৳)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Quantity
            OutlinedTextField(
                value = currentQuantity,
                onValueChange = onQuantityChange,
                label = { Text("Quantity") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Unit Dropdown
            var unitExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.weight(1f)
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
                    QuantityUnit.entries.forEach { unit ->
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

            // Add Button
            Button(
                onClick = onAddPrice,
                enabled = canAddPrice,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Price")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        // Duplicate Error
        AnimatedVisibility(visible = duplicateError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = duplicateError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// ==================== Section 3: Prices List ====================

@Composable
private fun BeveragePricesListSection(
    prices: List<BeveragePrice>,
    onRemovePrice: (Int) -> Unit
) {
    val df = remember { DecimalFormat("#,##0.##") }
    
    BeverageFormSection(title = "Price Options", icon = Icons.Outlined.Info) {
        if (prices.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Please add at least one price option",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prices.forEachIndexed { index, price ->
                    BeveragePriceCard(
                        price = price,
                        df = df,
                        onRemove = { onRemovePrice(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BeveragePriceCard(
    price: BeveragePrice,
    df: DecimalFormat,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 8.dp else 2.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .shadow(elevation, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price
                Column {
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "৳ ${df.format(price.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Quantity
                Column {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${df.format(price.quantity)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Unit
                Column {
                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = price.unit.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
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
}

// ==================== Action Buttons ====================

@Composable
private fun BeverageActionButtons(
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
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }

        Spacer(modifier = Modifier.width(16.dp))

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
private fun BeverageFormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
private fun BeverageErrorToast(message: String, modifier: Modifier = Modifier) {
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
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun BeverageSuccessToast(message: String, modifier: Modifier = Modifier) {
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
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}


