package ui.screens.takeout.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TakeoutDropdown(
    label: String,
    selected: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = enabled && expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(enabled && expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = enabled && expanded, onDismissRequest = { expanded = false }) {
            if (items.isEmpty()) {
                DropdownMenuItem(text = { Text("No option") }, onClick = { expanded = false }, enabled = false)
            } else {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(itemText(item)) }, onClick = { onSelect(item); expanded = false })
                }
            }
        }
    }
}
