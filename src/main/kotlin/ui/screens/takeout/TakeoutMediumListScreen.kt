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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.*
import ui.viewmodel.TakeoutUiEvent
import ui.viewmodel.TakeoutViewModel

@Composable
fun TakeoutMediumListScreen(
    viewModel: TakeoutViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<TakeoutMediumResponse?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.onEvent(TakeoutUiEvent.LoadAllMediums) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Takeout Mediums", style = MaterialTheme.typography.headlineSmall)
                Text("Configure platforms, required fields, and commission rules", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateBack) { Text("Back") }
                IconButton(onClick = { viewModel.onEvent(TakeoutUiEvent.LoadAllMediums) }) { Icon(Icons.Default.Refresh, null) }
                Button(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("New") }
            }
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.mediums, key = { it.id }) { medium ->
                TakeoutMediumCard(
                    medium = medium,
                    onEdit = { editing = medium; showDialog = true },
                    onDeactivate = { viewModel.onEvent(TakeoutUiEvent.DeleteMedium(medium.id)) }
                )
            }
        }
    }

    if (showDialog) {
        TakeoutMediumEditorDialog(
            medium = editing,
            onDismiss = { showDialog = false },
            onSave = { request ->
                viewModel.onEvent(TakeoutUiEvent.SaveMedium(editing?.id, request))
                showDialog = false
            }
        )
    }
}

@Composable
private fun TakeoutMediumCard(
    medium: TakeoutMediumResponse,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(medium.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(medium.mediumCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text(if (medium.active) "Available" else "Hidden from Order") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FriendlyChip(if (medium.platformBased) "Platform" else "Direct")
                FriendlyChip("Commission: ${medium.commissionType} ${medium.commissionValue}")
                medium.defaultPaymentMethod?.let { FriendlyChip("Default Pay: ${it.displayName}") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RequirementChip("External ID", medium.requiresExternalOrderId)
                RequirementChip("Phone", medium.requiresCustomerPhone)
                RequirementChip("Address", medium.requiresCustomerAddress)
                RequirementChip("Rider", medium.requiresRiderInfo)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) { Text("Edit") }
                if (medium.active) TextButton(onClick = onDeactivate) { Text("Hide") }
            }
        }
    }
}

@Composable
private fun FriendlyChip(text: String) {
    AssistChip(onClick = {}, label = { Text(text) })
}

@Composable
private fun RequirementChip(label: String, required: Boolean) {
    AssistChip(onClick = {}, label = { Text(if (required) "$label Required" else "$label Optional") })
}

@Composable
fun TakeoutMediumEditorDialog(
    medium: TakeoutMediumResponse?,
    onDismiss: () -> Unit,
    onSave: (TakeoutMediumRequest) -> Unit
) {
    var code by remember(medium) { mutableStateOf(medium?.mediumCode ?: "") }
    var name by remember(medium) { mutableStateOf(medium?.displayName ?: "") }
    var active by remember(medium) { mutableStateOf(medium?.active ?: true) }
    var platform by remember(medium) { mutableStateOf(medium?.platformBased ?: false) }
    var external by remember(medium) { mutableStateOf(medium?.requiresExternalOrderId ?: false) }
    var phone by remember(medium) { mutableStateOf(medium?.requiresCustomerPhone ?: false) }
    var address by remember(medium) { mutableStateOf(medium?.requiresCustomerAddress ?: false) }
    var rider by remember(medium) { mutableStateOf(medium?.requiresRiderInfo ?: false) }
    var commission by remember(medium) { mutableStateOf((medium?.commissionValue ?: 0.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (medium == null) "Create Medium" else "Edit Medium") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(code, { code = it.uppercase() }, label = { Text("Medium Code") })
                OutlinedTextField(name, { name = it }, label = { Text("Display Name") })
                MediumToggle("Available for new orders", active) { active = it }
                MediumToggle("Delivery platform", platform) { platform = it }
                MediumToggle("Require external order ID", external) { external = it }
                MediumToggle("Require customer phone", phone) { phone = it }
                MediumToggle("Require customer address", address) { address = it }
                MediumToggle("Require rider info", rider) { rider = it }
                OutlinedTextField(commission, { commission = it }, label = { Text("Commission Percentage") })
            }
        },
        confirmButton = {
            Button(
                enabled = code.isNotBlank() && name.isNotBlank(),
                onClick = {
                    onSave(
                        TakeoutMediumRequest(
                            mediumCode = code,
                            displayName = name,
                            active = active,
                            platformBased = platform,
                            requiresExternalOrderId = external,
                            requiresCustomerPhone = phone,
                            requiresCustomerAddress = address,
                            requiresRiderInfo = rider,
                            commissionType = if ((commission.toDoubleOrNull() ?: 0.0) > 0.0) CommissionType.PERCENTAGE else CommissionType.NONE,
                            commissionValue = commission.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MediumToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
