package ui.screens.takeout.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Gold = Color(0xFFD4AF37)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeoutReportDateTimePicker(
    dateValue: String,
    timeValue: String,
    onDateTimeChange: (String, String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = false)

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(18.dp), tint = Gold)
        Spacer(Modifier.width(8.dp))
        val display = if (dateValue.isBlank()) placeholder else if (timeValue.isBlank()) "$dateValue (All Day)" else {
            val formatted = runCatching { LocalTime.parse(timeValue, DateTimeFormatter.ofPattern("HH:mm")).format(DateTimeFormatter.ofPattern("hh:mm a")) }.getOrDefault(timeValue)
            "$dateValue, $formatted"
        }
        Text(
            text = display,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (dateValue.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        tempDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("UTC")).toLocalDate().toString()
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("Next") }
            },
            dismissButton = { OutlinedButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Time (Optional)", modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), fontWeight = FontWeight.SemiBold)
                    TimePicker(state = timePickerState, colors = TimePickerDefaults.colors(selectorColor = Gold, clockDialSelectedContentColor = Color(0xFF111827), timeSelectorSelectedContainerColor = Gold.copy(alpha = 0.15f), timeSelectorSelectedContentColor = Gold))
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDateTimeChange(tempDate, ""); showTimePicker = false }) { Text("Skip Time") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            val time = LocalTime.of(timePickerState.hour, timePickerState.minute).format(DateTimeFormatter.ofPattern("HH:mm"))
                            onDateTimeChange(tempDate, time)
                            showTimePicker = false
                        }) { Text("Confirm Time") }
                    }
                }
            }
        }
    }
}

fun takeoutReportIsoBoundary(date: String, time: String, isStart: Boolean): String? {
    if (date.isBlank()) return null
    val localDate = runCatching { java.time.LocalDate.parse(date) }.getOrNull() ?: return null
    val localTime = if (time.isBlank()) {
        if (isStart) java.time.LocalTime.MIN else java.time.LocalTime.of(23, 59, 59)
    } else runCatching { java.time.LocalTime.parse(time) }.getOrNull() ?: return null
    return java.time.OffsetDateTime.of(localDate, localTime, java.time.ZoneOffset.ofHours(6)).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
