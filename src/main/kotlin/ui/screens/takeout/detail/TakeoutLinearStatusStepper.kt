package ui.screens.takeout.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.model.TakeoutOrderStatus

private val Gold = Color(0xFFD4AF37)

@Composable
fun TakeoutLinearStatusStepper(current: TakeoutOrderStatus) {
    val statuses = listOf(
        TakeoutOrderStatus.ORDER_RECEIVED,
        TakeoutOrderStatus.ACCEPTED,
        TakeoutOrderStatus.PREPARING,
        TakeoutOrderStatus.READY_FOR_PICKUP,
        TakeoutOrderStatus.PICKED_UP,
        TakeoutOrderStatus.COMPLETED
    )
    val currentIndex = statuses.indexOf(current).coerceAtLeast(0)
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Box(Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 30.dp, vertical = 14.dp)) {
            Canvas(Modifier.fillMaxWidth().height(24.dp).align(Alignment.TopCenter).padding(horizontal = 48.dp)) {
                val y = size.height / 2
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                val progressWidth = if (statuses.size <= 1) 0f else size.width * (currentIndex.toFloat() / (statuses.size - 1).toFloat())
                drawLine(Gold, Offset(0f, y), Offset(progressWidth, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                statuses.forEachIndexed { index, status ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (index <= currentIndex) Gold else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(20.dp),
                            shadowElevation = if (status == current) 3.dp else 0.dp
                        ) {}
                        Spacer(Modifier.height(8.dp))
                        Text(
                            status.displayName,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (status == current) FontWeight.Black else FontWeight.SemiBold,
                            color = if (index <= currentIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (status == current) {
                            Spacer(Modifier.height(3.dp))
                            Surface(shape = MaterialTheme.shapes.small, color = Gold.copy(alpha = 0.16f)) {
                                Text("Current", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Gold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
