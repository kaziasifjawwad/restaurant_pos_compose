package ui.screens.takeout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.*
import ui.screens.takeout.detail.TakeoutDetailMetaBar
import ui.screens.takeout.detail.TakeoutFinancialSummary
import ui.screens.takeout.detail.TakeoutLinearStatusStepper
import ui.screens.takeout.detail.TakeoutOrderedItemsTable
import ui.screens.takeout.detail.TakeoutPartyInfoCard
import ui.screens.takeout.editor.TakeoutDropdown
import ui.viewmodel.TakeoutUiEvent
import ui.viewmodel.TakeoutViewModel

private val Gold = Color(0xFFD4AF37)

@Composable
fun TakeoutOrderDetailScreen(
    orderId: Long,
    viewModel: TakeoutViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    readOnly: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    val order = state.selectedOrder

    LaunchedEffect(orderId) {
        if (!readOnly) viewModel.onEvent(TakeoutUiEvent.LoadLookupData)
        viewModel.onEvent(TakeoutUiEvent.LoadOrderDetail(orderId))
    }

    Scaffold(topBar = { TakeoutDetailTopBar(order?.takeoutOrderNumber ?: "TO-$orderId", onNavigateBack, readOnly) }) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize().background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)))
            )
        ) {
            when {
                state.isLoadingDetail || order == null -> TakeoutDetailLoading()
                state.errorMessage != null -> TakeoutDetailError(state.errorMessage.orEmpty(), onNavigateBack)
                else -> TakeoutDetailContent(order, state.paymentMethods, viewModel, onNavigateToEdit, readOnly)
            }
        }
    }
}

@Composable
private fun TakeoutDetailTopBar(orderNumber: String, onNavigateBack: () -> Unit, readOnly: Boolean) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Gold) }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (readOnly) "Takeout Report Order View" else "Takeout Order Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(orderNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TakeoutDetailContent(
    order: TakeoutOrderResponse,
    paymentMethods: List<PaymentMethodResponse>,
    viewModel: TakeoutViewModel,
    onNavigateToEdit: (Long) -> Unit,
    readOnly: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp)
    ) {
        item { TakeoutLinearStatusStepper(order.orderStatus) }
        item { TakeoutDetailMetaBar(order) }
        if (!readOnly) {
            item { TakeoutActionBar(order, viewModel, onNavigateToEdit) }
            item { TakeoutPaymentSection(order, paymentMethods, viewModel) }
        } else {
            item { TakeoutReadOnlyPaymentInfo(order) }
        }
        item { TakeoutPartyInfoCard(order) }
        item { TakeoutOrderedItemsTable(order) }
        item { TakeoutFinancialSummary(order) }
    }
}

@Composable
private fun TakeoutActionBar(order: TakeoutOrderResponse, viewModel: TakeoutViewModel, onNavigateToEdit: (Long) -> Unit) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ORDER ACTIONS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!order.orderStatus.isFinal) {
                    OutlinedButton(onClick = { onNavigateToEdit(order.id) }) { Icon(Icons.Filled.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Edit Items") }
                    StatusButton("Accept", order, TakeoutOrderStatus.ACCEPTED, viewModel)
                    StatusButton("Begin Preparation", order, TakeoutOrderStatus.PREPARING, viewModel)
                    StatusButton("Ready", order, TakeoutOrderStatus.READY_FOR_PICKUP, viewModel)
                    StatusButton("Picked Up", order, TakeoutOrderStatus.PICKED_UP, viewModel)
                    CompletePaidButton(order, viewModel)
                    TextButton(onClick = { showCancelDialog = true }) { Icon(Icons.Filled.Cancel, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Cancel Order") }
                }
            }
        }
    }
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel takeout order?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("This action will cancel the order. Are you sure?"); OutlinedTextField(cancelReason, { cancelReason = it }, label = { Text("Cancel reason") }, minLines = 2) } },
            confirmButton = { Button(enabled = cancelReason.isNotBlank(), onClick = { viewModel.onEvent(TakeoutUiEvent.CancelOrder(order.id, cancelReason)); showCancelDialog = false }) { Text("Yes, Cancel") } },
            dismissButton = { OutlinedButton(onClick = { showCancelDialog = false }) { Text("No") } }
        )
    }
}

@Composable
private fun StatusButton(label: String, order: TakeoutOrderResponse, target: TakeoutOrderStatus, viewModel: TakeoutViewModel) {
    val enabled = !order.orderStatus.isFinal && order.orderStatus != target
    OutlinedButton(enabled = enabled, onClick = { viewModel.onEvent(TakeoutUiEvent.UpdateStatus(order.id, target)) }) { Text(label) }
}

@Composable
private fun CompletePaidButton(order: TakeoutOrderResponse, viewModel: TakeoutViewModel) {
    val paid = order.paymentMethod != null && order.paymentStatus == TakeoutPaymentStatus.PAID
    Button(enabled = paid && !order.orderStatus.isFinal, onClick = { viewModel.onEvent(TakeoutUiEvent.UpdateStatus(order.id, TakeoutOrderStatus.COMPLETED)) }) {
        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (paid) "Complete & Paid" else "Select Payment First")
    }
}

@Composable
private fun TakeoutPaymentSection(order: TakeoutOrderResponse, paymentMethods: List<PaymentMethodResponse>, viewModel: TakeoutViewModel) {
    var selected by remember(order.id, order.paymentMethod, paymentMethods) { mutableStateOf(order.paymentMethod ?: paymentMethods.firstOrNull { it.active }?.methodCode) }
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("PAYMENT METHOD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TakeoutDropdown(label = "Payment Method", selected = selected?.displayName ?: "Select payment method", items = paymentMethods.filter { it.active }.map { it.methodCode }, itemText = { it.displayName }, onSelect = { selected = it }, modifier = Modifier.weight(1f))
                Button(enabled = selected != null && !order.orderStatus.isFinal, onClick = { viewModel.onEvent(TakeoutUiEvent.UpdatePayment(order.id, TakeoutPaymentUpdateRequest(paymentMethod = selected, paymentStatus = TakeoutPaymentStatus.PAID, settlementStatus = order.settlementStatus))) }) {
                    Text(if (order.paymentStatus == TakeoutPaymentStatus.PAID) "Update Paid Method" else "Mark Paid")
                }
            }
            Text("Current: ${order.paymentMethod?.displayName ?: "N/A"} • ${order.paymentStatus.displayName}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TakeoutReadOnlyPaymentInfo(order: TakeoutOrderResponse) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PAYMENT METHOD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text("Method: ${order.paymentMethod?.displayName ?: "N/A"}")
            Text("Payment: ${order.paymentStatus.displayName} • Settlement: ${order.settlementStatus.displayName}")
        }
    }
}

@Composable private fun TakeoutDetailLoading() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) } }
@Composable private fun TakeoutDetailError(errorMessage: String, onNavigateBack: () -> Unit) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(errorMessage, color = MaterialTheme.colorScheme.error); Button(onClick = onNavigateBack) { Text("Go Back") } } } }
