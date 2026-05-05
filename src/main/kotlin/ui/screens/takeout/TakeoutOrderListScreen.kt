package ui.screens.takeout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.model.TakeoutOrderShortInfo
import ui.viewmodel.TakeoutUiEvent
import ui.viewmodel.TakeoutViewModel

@Composable
fun TakeoutOrderListScreen(
    viewModel: TakeoutViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToMediums: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onEvent(TakeoutUiEvent.LoadLookupData)
        viewModel.onEvent(TakeoutUiEvent.LoadActiveOrders)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Takeout Orders", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateToMediums) { Text("Mediums") }
                IconButton(onClick = { viewModel.onEvent(TakeoutUiEvent.RefreshActiveOrders) }) { Icon(Icons.Default.Refresh, null) }
                Button(onClick = onNavigateToCreate) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New") }
            }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))
        if (state.isLoadingOrders) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.activeOrders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No active takeout orders") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.activeOrders, key = { it.id }) { order ->
                    TakeoutOrderCard(
                        order = order,
                        canEdit = viewModel.canEditOrder(order),
                        onView = { onNavigateToDetail(order.id) },
                        onEdit = { onNavigateToEdit(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TakeoutOrderCard(
    order: TakeoutOrderShortInfo,
    canEdit: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.takeoutOrderNumber ?: "TO-${order.id}", style = MaterialTheme.typography.titleMedium)
                AssistChip(onClick = {}, label = { Text(order.orderStatus.displayName) })
            }
            Text("${order.mediumName ?: order.mediumCode} • External: ${order.externalOrderId ?: "N/A"}")
            Text("Customer: ${order.customerName ?: "N/A"} • Phone: ${order.customerPhone ?: "N/A"}")
            Text("Payment: ${order.paymentStatus.displayName} • Method: ${order.paymentMethod?.displayName ?: "N/A"}")
            Text("Total: ৳ ${"%.2f".format(order.totalAmount)} • Commission: ৳ ${"%.2f".format(order.platformCommissionAmount)} • Receivable: ৳ ${"%.2f".format(order.restaurantReceivableAmount)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onView) { Text("View") }
                if (canEdit) OutlinedButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}
