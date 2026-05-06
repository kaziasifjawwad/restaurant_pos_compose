package ui.screens.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.PrinterRequest
import data.model.PrinterResponse
import data.model.Result
import data.repository.PosRepository
import kotlinx.coroutines.launch

@Composable
fun PrinterSettingsScreen(repository: PosRepository = PosRepository.getInstance()) {
    val scope = rememberCoroutineScope()
    var printers by remember { mutableStateOf<List<PrinterResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingPrinter by remember { mutableStateOf<PrinterResponse?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    fun loadPrinters() {
        scope.launch {
            isLoading = true
            when (val result = repository.getAllPrinters()) {
                is Result.Success -> printers = result.data
                is Result.Error -> message = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadPrinters() }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Printer Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Save printer model names and select one default printer for all POS memos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { loadPrinters() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                    Button(onClick = { editingPrinter = null; showEditor = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Printer")
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (printers.isEmpty()) {
                EmptyPrinterState(onAdd = { editingPrinter = null; showEditor = true })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(printers, key = { it.id }) { printer ->
                        PrinterCard(
                            printer = printer,
                            onSetDefault = {
                                scope.launch {
                                    when (val result = repository.setDefaultPrinter(printer.id)) {
                                        is Result.Success -> { message = "Default printer set to ${result.data.printerModelName}"; loadPrinters() }
                                        is Result.Error -> message = result.message
                                    }
                                }
                            },
                            onEdit = { editingPrinter = printer; showEditor = true },
                            onDelete = {
                                scope.launch {
                                    when (val result = repository.deletePrinter(printer.id)) {
                                        is Result.Success -> { message = "Printer deleted"; loadPrinters() }
                                        is Result.Error -> message = result.message
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        PrinterEditorDialog(
            printer = editingPrinter,
            onDismiss = { showEditor = false },
            onSave = { request ->
                scope.launch {
                    val result = editingPrinter?.let { repository.updatePrinter(it.id, request) }
                        ?: repository.createPrinter(request)
                    when (result) {
                        is Result.Success -> { message = "Printer saved"; showEditor = false; loadPrinters() }
                        is Result.Error -> message = result.message
                    }
                }
            }
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Printer Settings") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun EmptyPrinterState(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("No printer configured", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Add the exact Windows printer name/model shown in Devices & Printers.")
            Button(onClick = onAdd) { Text("Add Printer") }
        }
    }
}

@Composable
private fun PrinterCard(
    printer: PrinterResponse,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val border = if (printer.defaultPrinter) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(12.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(printer.printerModelName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (printer.active) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (printer.defaultPrinter) {
                    FilledTonalButton(onClick = {}) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Default")
                    }
                } else {
                    OutlinedButton(enabled = printer.active, onClick = onSetDefault) { Text("Set Default") }
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Composable
private fun PrinterEditorDialog(
    printer: PrinterResponse?,
    onDismiss: () -> Unit,
    onSave: (PrinterRequest) -> Unit
) {
    var printerModelName by remember(printer?.id) { mutableStateOf(printer?.printerModelName.orEmpty()) }
    var active by remember(printer?.id) { mutableStateOf(printer?.active ?: true) }
    var defaultPrinter by remember(printer?.id) { mutableStateOf(printer?.defaultPrinter ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (printer == null) "Add Printer" else "Edit Printer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = printerModelName,
                    onValueChange = { printerModelName = it },
                    label = { Text("Printer model/name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text("Active")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = defaultPrinter, onCheckedChange = { defaultPrinter = it })
                    Text("Use as default POS printer")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = printerModelName.isNotBlank(),
                onClick = { onSave(PrinterRequest(printerModelName.trim(), defaultPrinter, active)) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
