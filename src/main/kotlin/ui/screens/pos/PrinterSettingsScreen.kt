package ui.screens.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import data.model.PrinterResponse
import data.model.Result
import data.repository.PosRepository
import kotlinx.coroutines.launch

@Composable
fun PrinterSettingsScreen(repository: PosRepository = PosRepository.getInstance()) {
    val scope = rememberCoroutineScope()
    var configuredPrinters by remember { mutableStateOf<List<PrinterResponse>>(emptyList()) }
    var systemPrinters by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedPrinterName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun loadPrinters() {
        scope.launch {
            isLoading = true
            when (val configuredResult = repository.getAllPrinters()) {
                is Result.Success -> configuredPrinters = configuredResult.data
                is Result.Error -> message = configuredResult.message
            }
            when (val systemResult = repository.refreshSystemPrinters()) {
                is Result.Success -> {
                    systemPrinters = systemResult.data
                    val currentDefault = configuredPrinters.firstOrNull { it.active && it.defaultPrinter }?.printerModelName
                    selectedPrinterName = currentDefault ?: systemResult.data.firstOrNull()
                }
                is Result.Error -> message = systemResult.message
            }
            isLoading = false
        }
    }

    fun saveSelectedPrinter() {
        val selectedName = selectedPrinterName?.trim().orEmpty()
        if (selectedName.isBlank()) {
            message = "Select a printer first"
            return
        }
        scope.launch {
            isSaving = true
            when (val result = repository.saveSystemPrinterAsDefault(selectedName)) {
                is Result.Success -> {
                    message = "Default printer saved: ${result.data.printerModelName}"
                    loadPrinters()
                }
                is Result.Error -> message = result.message
            }
            isSaving = false
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
                        "Select one printer detected from this system and save it as the default POS memo printer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(enabled = !isLoading && !isSaving, onClick = { loadPrinters() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                    Button(enabled = !isLoading && !isSaving && selectedPrinterName != null, onClick = { saveSelectedPrinter() }) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Save Default")
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (systemPrinters.isEmpty()) {
                EmptyPrinterState(onRefresh = { loadPrinters() })
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(systemPrinters, key = { it }) { printerName ->
                        val configuredPrinter = configuredPrinters.firstOrNull {
                            it.printerModelName.equals(printerName, ignoreCase = true)
                        }
                        SystemPrinterCard(
                            printerName = printerName,
                            configuredPrinter = configuredPrinter,
                            selected = selectedPrinterName == printerName,
                            onSelect = { selectedPrinterName = printerName }
                        )
                    }
                }
            }
        }
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
private fun EmptyPrinterState(onRefresh: () -> Unit) {
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
            Text("No system printer found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Install or connect a printer on this computer, then refresh this page.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRefresh) { Text("Refresh Printers") }
        }
    }
}

@Composable
private fun SystemPrinterCard(
    printerName: String,
    configuredPrinter: PrinterResponse?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val isDefault = configuredPrinter?.defaultPrinter == true && configuredPrinter.active
    val border = if (selected || isDefault) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
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
                RadioButton(selected = selected, onClick = onSelect)
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(12.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(printerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            isDefault -> "Saved as default POS printer"
                            configuredPrinter?.active == true -> "Saved in database"
                            configuredPrinter != null -> "Saved but inactive; saving will reactivate it"
                            else -> "Detected from this system; saving will add it to database"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isDefault) {
                FilledTonalButton(onClick = onSelect) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Default")
                }
            }
        }
    }
}
