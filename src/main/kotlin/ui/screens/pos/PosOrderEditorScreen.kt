package ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import data.model.*
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import ui.viewmodel.PosUiEvent
import ui.viewmodel.PosUiState
import ui.viewmodel.PosViewModel
import java.text.DecimalFormat

/**
 * POS Order Editor Screen - Create or Edit orders
 */
@Composable
fun PosOrderEditorScreen(
    orderId: Long? = null,
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = orderId != null
    var isContentVisible by remember { mutableStateOf(false) }

    // Editor state
    var selectedWaiter by remember { mutableStateOf<WaiterInfo?>(null) }
    var selectedTable by remember { mutableStateOf<TableInfo?>(null) }
    var foodOrders by remember { mutableStateOf<List<FoodOrderEntry>>(emptyList()) }
    var beverageOrders by remember { mutableStateOf<List<BeverageOrderEntry>>(emptyList()) }

    // Food input state. Null means: use backend default package for that food item.
    var foodSerialInput by remember { mutableStateOf("") }
    var selectedFoodSize by remember { mutableStateOf<FoodSize?>(null) }

    // Beverage input state
    var selectedBeverage by remember { mutableStateOf<BeverageResponse?>(null) }
    var selectedBeveragePrice by remember { mutableStateOf<BeveragePrice?>(null) }
    var beverageAmountInput by remember { mutableStateOf("1") }

    LaunchedEffect(orderId) {
        if (isEditMode) {
            viewModel.onEvent(PosUiEvent.OpenEditEditor(orderId!!))
        } else {
            viewModel.onEvent(PosUiEvent.OpenCreateEditor)
        }
        delay(100)
        isContentVisible = true
    }

    // Populate from loaded order when editing
    LaunchedEffect(uiState.selectedOrder) {
        uiState.selectedOrder?.let { order ->
            if (isEditMode) {
                selectedWaiter = uiState.waiters.find { it.id == order.waiterId }
                selectedTable = uiState.tables.find { it.id == order.tableId }
                foodOrders = order.foodOrders.map { fo ->
                    FoodOrderEntry(
                        itemNumber = fo.itemNumber,
                        foodName = fo.foodName ?: "Food #${fo.itemNumber}",
                        actualFoodSize = fo.foodSize,
                        requestedFoodSize = fo.foodSize,
                        quantity = fo.foodQuantity,
                        price = fo.foodPrice,
                        discount = fo.discount,
                        discountType = fo.discountType ?: DiscountType.PERCENTAGE
                    )
                }
                beverageOrders = order.beverageOrders.map { bo ->
                    BeverageOrderEntry(
                        beverageId = bo.beverageId ?: 0L,
                        beverageName = bo.beverageName ?: "Beverage #${bo.beverageId}",
                        quantity = bo.quantity,
                        amount = bo.amount,
                        unit = bo.unit ?: QuantityUnit.MILLILITER,
                        price = bo.price,
                        discount = bo.discount,
                        discountType = bo.discountType ?: DiscountType.PERCENTAGE
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null && uiState.editorMode == PosUiState.EditorMode.Closed) {
            delay(500)
            viewModel.onEvent(PosUiEvent.ClearSuccess)
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(3000)
            viewModel.onEvent(PosUiEvent.ClearError)
        }
    }

    fun addFoodItem() {
        val textRepresentation = foodSerialInput.trim()
        if (textRepresentation.isBlank()) {
            viewModel.onEvent(PosUiEvent.ShowError("Please enter item number"))
            return
        }

        val regexPattern = "^(?:\\d+|(?:\\d+\\s*\\*\\s*\\d+))(?:\\s+(?:\\d+|(?:\\d+\\s*\\*\\s*\\d+)))*$"
        if (!textRepresentation.matches(Regex(regexPattern))) {
            viewModel.onEvent(PosUiEvent.ShowError("Invalid pattern. Use: 1 or 1*3 or 1 2*3"))
            return
        }

        val orderItems = textRepresentation.split(" ").map { it.split("*") }

        orderItems.forEach { order ->
            val itemNumber: Short
            val quantity: Int

            try {
                if (order.size == 1) {
                    itemNumber = order[0].toShort()
                    quantity = 1
                } else {
                    itemNumber = order[0].toShort()
                    quantity = order[1].toInt()
                }
            } catch (e: NumberFormatException) {
                viewModel.onEvent(PosUiEvent.ShowError("Invalid number format"))
                return@forEach
            }

            val foodItem = viewModel.getFoodItemByNumber(itemNumber)
            if (foodItem == null) {
                viewModel.onEvent(PosUiEvent.ShowError("Item #$itemNumber not found"))
                return@forEach
            }

            val priceInfo = selectedFoodSize?.let { selectedSize ->
                foodItem.foodPrices.find { it.foodSize == selectedSize }
            } ?: foodItem.defaultPrice ?: foodItem.foodPrices.firstOrNull { it.isDefault }

            if (priceInfo == null) {
                val sizeLabel = selectedFoodSize?.name ?: "default package"
                viewModel.onEvent(PosUiEvent.ShowError("${foodItem.name} has no $sizeLabel price configured"))
                return@forEach
            }

            foodOrders = foodOrders.filter {
                !(it.itemNumber == itemNumber && it.requestedFoodSize == selectedFoodSize)
            }

            foodOrders = foodOrders + FoodOrderEntry(
                itemNumber = itemNumber,
                foodName = foodItem.name,
                actualFoodSize = priceInfo.foodSize,
                requestedFoodSize = selectedFoodSize,
                quantity = quantity,
                price = priceInfo.foodPrice,
                discount = 0.0,
                discountType = DiscountType.PERCENTAGE
            )
        }

        foodSerialInput = ""
    }

    fun addBeverageItem() {
        val bev = selectedBeverage ?: return
        val price = selectedBeveragePrice ?: return
        val amount = beverageAmountInput.toIntOrNull() ?: 1

        beverageOrders = beverageOrders.filter {
            !(it.beverageId == bev.id && it.quantity == price.quantity && it.unit == price.unit)
        }
        beverageOrders = beverageOrders + BeverageOrderEntry(
            beverageId = bev.id,
            beverageName = "${bev.name} ${price.quantity}${price.unit.name.first()}",
            quantity = price.quantity,
            amount = amount,
            unit = price.unit,
            price = price.price,
            discount = 0.0,
            discountType = DiscountType.PERCENTAGE
        )
        beverageAmountInput = "1"
        selectedBeveragePrice = null
    }

    fun submitOrder() {
        val waiter = selectedWaiter ?: return
        val table = selectedTable ?: return
        if (foodOrders.isEmpty() && beverageOrders.isEmpty()) return

        val request = FoodOrderByCustomerRequest(
            id = orderId,
            waiterId = waiter.id,
            waiterName = waiter.displayName,
            orderStatus = OrderStatus.ORDER_PLACED,
            tableId = table.id,
            discountType = DiscountType.PERCENTAGE,
            discount = 0.0,
            foodOrders = foodOrders.map { fo ->
                FoodOrderRequest(
                    itemNumber = fo.itemNumber,
                    foodSize = fo.requestedFoodSize,
                    foodQuantity = fo.quantity,
                    discount = fo.discount,
                    discountType = fo.discountType
                )
            },
            beverageOrders = beverageOrders.map { bo ->
                BeverageOrderRequest(
                    beverageId = bo.beverageId,
                    amount = bo.amount,
                    quantity = bo.quantity,
                    unit = bo.unit,
                    discount = bo.discount,
                    discountType = bo.discountType
                )
            }
        )

        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        println(json.encodeToString(request))
        viewModel.onEvent(PosUiEvent.SaveOrder(request))
    }

    val totalAmount = remember(foodOrders, beverageOrders) {
        val foodTotal = foodOrders.sumOf { it.price * it.quantity }
        val bevTotal = beverageOrders.sumOf { it.price * it.amount }
        foodTotal + bevTotal
    }

    val df = remember { DecimalFormat("#,##0.00") }

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
            PosEditorHeader(
                isEditMode = isEditMode,
                onBack = {
                    viewModel.onEvent(PosUiEvent.CloseEditor)
                    onNavigateBack()
                }
            )

            if (uiState.isLoadingLookups || (isEditMode && uiState.isLoadingDetail)) {
                PosEditorLoadingContent()
            } else {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(animationSpec = tween(AppAnimations.DURATION_ENTRANCE))
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "Order Items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (foodOrders.isEmpty() && beverageOrders.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.ShoppingCart,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "No items added yet",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(foodOrders) { index, entry ->
                                PosEditorItemCard(
                                    name = entry.foodName,
                                    subtitle = entry.packageLabel,
                                    price = entry.price * entry.quantity,
                                    df = df,
                                    onRemove = { foodOrders = foodOrders.filterIndexed { i, _ -> i != index } }
                                )
                            }

                            itemsIndexed(beverageOrders) { index, entry ->
                                PosEditorItemCard(
                                    name = entry.beverageName,
                                    subtitle = "${entry.quantity} ${entry.unit.name} × ${entry.amount}",
                                    price = entry.price * entry.amount,
                                    df = df,
                                    onRemove = { beverageOrders = beverageOrders.filterIndexed { i, _ -> i != index } }
                                )
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Total",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "৳ ${df.format(totalAmount)}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PosEditorFormSection(title = "Waiter & Table") {
                                PosDropdown(
                                    label = "Waiter",
                                    selected = selectedWaiter?.displayName ?: "",
                                    items = uiState.waiters,
                                    itemText = { it.displayName },
                                    onSelect = { selectedWaiter = it }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PosDropdown(
                                    label = "Table",
                                    selected = selectedTable?.let { "Table #${it.tableNumber}" } ?: "",
                                    items = uiState.tables,
                                    itemText = { "Table #${it.tableNumber}" },
                                    onSelect = { selectedTable = it }
                                )
                            }

                            PosEditorFormSection(title = "Add Food Item") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    OutlinedTextField(
                                        value = foodSerialInput,
                                        onValueChange = { foodSerialInput = it },
                                        label = { Text("Item # or 1*3 or 1 2*3") },
                                        supportingText = { Text("Leave package as Default for fastest ordering") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    PosDropdown(
                                        label = "Package",
                                        selected = selectedFoodSize?.name ?: "DEFAULT",
                                        items = listOf<FoodSize?>(null) + FoodSize.entries.toList(),
                                        itemText = { it?.name ?: "DEFAULT" },
                                        onSelect = { selectedFoodSize = it },
                                        modifier = Modifier.width(124.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { addFoodItem() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = foodSerialInput.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Food")
                                }
                            }

                            PosEditorFormSection(title = "Add Beverage") {
                                PosDropdown(
                                    label = "Beverage",
                                    selected = selectedBeverage?.name ?: "",
                                    items = uiState.beverages,
                                    itemText = { it.name },
                                    onSelect = {
                                        selectedBeverage = it
                                        selectedBeveragePrice = null
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    PosDropdown(
                                        label = "Size",
                                        selected = selectedBeveragePrice?.let { "${it.quantity}${it.unit.name.first()}" } ?: "",
                                        items = selectedBeverage?.prices ?: emptyList(),
                                        itemText = { "${it.quantity} ${it.unit.name}" },
                                        onSelect = { selectedBeveragePrice = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = beverageAmountInput,
                                        onValueChange = { if (it.all { c -> c.isDigit() }) beverageAmountInput = it },
                                        label = { Text("Qty") },
                                        modifier = Modifier.width(80.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { addBeverageItem() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = selectedBeverage != null && selectedBeveragePrice != null
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Beverage")
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.onEvent(PosUiEvent.CloseEditor)
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Discard")
                                }

                                Button(
                                    onClick = { submitOrder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = selectedWaiter != null &&
                                            selectedTable != null &&
                                            (foodOrders.isNotEmpty() || beverageOrders.isNotEmpty()) &&
                                            !uiState.isSaving
                                ) {
                                    if (uiState.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEditMode) "Update" else "Submit")
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PosToast(
                message = uiState.errorMessage ?: "",
                isError = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// Entry classes for editor state
private data class FoodOrderEntry(
    val itemNumber: Short,
    val foodName: String,
    val actualFoodSize: FoodSize,
    val requestedFoodSize: FoodSize?,
    val quantity: Int,
    val price: Double,
    val discount: Double,
    val discountType: DiscountType
) {
    val packageLabel: String
        get() = if (requestedFoodSize == null) {
            "Default: ${actualFoodSize.name} × $quantity"
        } else {
            "${actualFoodSize.name} × $quantity"
        }
}

private data class BeverageOrderEntry(
    val beverageId: Long,
    val beverageName: String,
    val quantity: Double,
    val amount: Int,
    val unit: QuantityUnit,
    val price: Double,
    val discount: Double,
    val discountType: DiscountType
)

@Composable
private fun PosEditorHeader(
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
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isEditMode) "Edit Order" else "New Order",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isEditMode) "Update order details" else "Place a new order",
                    style = ExtendedTypography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PosEditorLoadingContent() {
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

@Composable
private fun PosEditorFormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PosDropdown(
    label: String,
    selected: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemText(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PosEditorItemCard(
    name: String,
    subtitle: String,
    price: Double,
    df: DecimalFormat,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "৳ ${df.format(price)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PosToast(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
