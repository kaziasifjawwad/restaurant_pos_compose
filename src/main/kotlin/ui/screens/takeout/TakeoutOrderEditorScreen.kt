package ui.screens.takeout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.*
import ui.screens.takeout.editor.*
import ui.viewmodel.TakeoutUiEvent
import ui.viewmodel.TakeoutUiState
import ui.viewmodel.TakeoutViewModel

@Composable
fun TakeoutOrderEditorScreen(
    orderId: Long?,
    viewModel: TakeoutViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var mediumCode by remember { mutableStateOf("") }
    var externalOrderId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var riderName by remember { mutableStateOf("") }
    var riderPhone by remember { mutableStateOf("") }
    var riderPickupCode by remember { mutableStateOf("") }
    var packagingCharge by remember { mutableStateOf("0") }
    var deliveryCharge by remember { mutableStateOf("0") }
    var specialInstruction by remember { mutableStateOf("") }
    var foodOrders by remember { mutableStateOf<List<TakeoutFoodEntry>>(emptyList()) }
    var beverageOrders by remember { mutableStateOf<List<TakeoutBeverageEntry>>(emptyList()) }
    var navigatingAfterSave by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        viewModel.onEvent(TakeoutUiEvent.LoadLookupData)
        if (orderId == null) viewModel.onEvent(TakeoutUiEvent.OpenCreateEditor)
        else viewModel.onEvent(TakeoutUiEvent.OpenEditEditor(orderId))
    }

    LaunchedEffect(state.editorMode, state.successMessage) {
        if (!navigatingAfterSave && state.successMessage == "Takeout order saved" && state.editorMode is TakeoutUiState.EditorMode.Closed) {
            navigatingAfterSave = true
            viewModel.onEvent(TakeoutUiEvent.RefreshActiveOrders)
            onNavigateBack()
        }
    }

    LaunchedEffect(state.mediums) {
        if (mediumCode.isBlank() && state.mediums.isNotEmpty()) {
            mediumCode = state.mediums.first().mediumCode
        }
    }

    LaunchedEffect(state.selectedOrder) {
        state.selectedOrder?.let { order ->
            mediumCode = order.mediumCode
            externalOrderId = order.externalOrderId.orEmpty()
            customerName = order.customerName.orEmpty()
            customerPhone = order.customerPhone.orEmpty()
            customerAddress = order.customerAddress.orEmpty()
            riderName = order.riderName.orEmpty()
            riderPhone = order.riderPhone.orEmpty()
            riderPickupCode = order.riderPickupCode.orEmpty()
            packagingCharge = order.packagingCharge.toString()
            deliveryCharge = order.deliveryCharge.toString()
            specialInstruction = order.specialInstruction.orEmpty()
            foodOrders = order.foodOrders.mapNotNull { line ->
                val itemNo = line.itemNumber ?: return@mapNotNull null
                val size = line.foodSize ?: return@mapNotNull null
                TakeoutFoodEntry(itemNo, line.foodName ?: "Food #$itemNo", size, null, line.foodQuantity, line.foodPrice, line.packagingInstruction)
            }
            beverageOrders = order.beverageOrders.mapNotNull { line ->
                val id = line.beverageId ?: return@mapNotNull null
                val unit = line.unit ?: return@mapNotNull null
                TakeoutBeverageEntry(id, line.beverageName ?: "Beverage #$id", line.quantity, line.amount, unit, line.price, line.packagingInstruction)
            }
        }
    }

    val selectedMedium = state.mediums.firstOrNull { it.mediumCode == mediumCode }
    val validationError = validateTakeoutOrderFields(
        medium = selectedMedium,
        externalOrderId = externalOrderId,
        customerPhone = customerPhone,
        customerAddress = customerAddress,
        riderName = riderName,
        riderPhone = riderPhone
    )
    val hasItems = foodOrders.isNotEmpty() || beverageOrders.isNotEmpty()
    val total = foodOrders.sumOf { it.lineTotal } + beverageOrders.sumOf { it.lineTotal }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(if (orderId == null) "Create Takeout Order" else "Edit Takeout Order", style = MaterialTheme.typography.headlineSmall)
                Text("Add food and beverage separately. No waiter/table needed.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onNavigateBack) { Text("Back") }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        if (submitAttempted) submitError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }

        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LazyColumn(Modifier.weight(0.62f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Order Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (!hasItems) {
                    item { Text("No food or beverage added yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                itemsIndexed(foodOrders) { index, entry ->
                    TakeoutEditorItemCard(entry.foodName, entry.packageLabel, entry.lineTotal) {
                        foodOrders = foodOrders.filterIndexed { i, _ -> i != index }
                    }
                }
                itemsIndexed(beverageOrders) { index, entry ->
                    TakeoutEditorItemCard(entry.beverageName, "${entry.quantity} ${entry.unit.name} × ${entry.amount}", entry.lineTotal) {
                        beverageOrders = beverageOrders.filterIndexed { i, _ -> i != index }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Items Total", fontWeight = FontWeight.Bold)
                            Text("৳ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            LazyColumn(Modifier.weight(0.38f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    TakeoutSourceCustomerSection(
                        mediums = state.mediums,
                        mediumCode = mediumCode,
                        onMediumCodeChange = { mediumCode = it; submitError = null },
                        externalOrderId = externalOrderId,
                        onExternalOrderIdChange = { externalOrderId = it; submitError = null },
                        customerName = customerName,
                        onCustomerNameChange = { customerName = it },
                        customerPhone = customerPhone,
                        onCustomerPhoneChange = { customerPhone = it; submitError = null },
                        customerAddress = customerAddress,
                        onCustomerAddressChange = { customerAddress = it; submitError = null }
                    )
                }
                item {
                    TakeoutAddFoodSection(
                        foodItems = state.foodItems,
                        onAdd = { entry ->
                            foodOrders = foodOrders.filterNot { it.itemNumber == entry.itemNumber && it.actualFoodSize == entry.actualFoodSize } + entry
                            submitError = null
                        },
                        onError = { viewModel.onEvent(TakeoutUiEvent.ShowError(it)) }
                    )
                }
                item {
                    TakeoutAddBeverageSection(
                        beverages = state.beverages,
                        onAdd = { entry ->
                            beverageOrders = beverageOrders.filterNot { it.beverageId == entry.beverageId && it.quantity == entry.quantity && it.unit == entry.unit } + entry
                            submitError = null
                        }
                    )
                }
                item {
                    TakeoutRiderChargeSection(
                        riderName, { riderName = it; submitError = null },
                        riderPhone, { riderPhone = it; submitError = null },
                        riderPickupCode, { riderPickupCode = it },
                        packagingCharge, { packagingCharge = it },
                        deliveryCharge, { deliveryCharge = it },
                        specialInstruction, { specialInstruction = it }
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onNavigateBack, modifier = Modifier.weight(1f)) { Text("Discard") }
                        Button(
                            enabled = !state.isSaving,
                            onClick = {
                                submitAttempted = true
                                submitError = when {
                                    !hasItems -> "Add at least one food or beverage item"
                                    validationError != null -> validationError
                                    else -> null
                                }
                                if (submitError == null) {
                                    viewModel.onEvent(
                                        TakeoutUiEvent.SaveOrder(
                                            TakeoutOrderRequest(
                                                mediumCode = mediumCode,
                                                externalOrderId = externalOrderId.ifBlank { null },
                                                customerName = customerName.ifBlank { null },
                                                customerPhone = customerPhone.ifBlank { null },
                                                customerAddress = customerAddress.ifBlank { null },
                                                riderName = riderName.ifBlank { null },
                                                riderPhone = riderPhone.ifBlank { null },
                                                riderPickupCode = riderPickupCode.ifBlank { null },
                                                specialInstruction = specialInstruction.ifBlank { null },
                                                packagingCharge = packagingCharge.toDoubleOrNull() ?: 0.0,
                                                deliveryCharge = deliveryCharge.toDoubleOrNull() ?: 0.0,
                                                foodOrders = foodOrders.toFoodRequests(),
                                                beverageOrders = beverageOrders.toBeverageRequests()
                                            )
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.isSaving) "Saving..." else "Submit")
                        }
                    }
                }
            }
        }
    }
}
