package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.model.FoodItemResponse
import data.model.FoodPrice
import ui.theme.AppAnimations
import ui.theme.ExtendedTypography
import java.text.DecimalFormat

@Composable
fun FoodCard(
    foodItem: FoodItemResponse,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // Animated values for hover effect
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 16.dp else 4.dp,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = tween(AppAnimations.DURATION_FAST, easing = AppAnimations.EaseOutQuart)
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
        else 
            Color.Transparent,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(240.dp)
            .hoverable(interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header with item number and gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item number badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "#${foodItem.itemNumber}",
                            style = ExtendedTypography.menuItemNumber,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Food icon
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Food name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Divider with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Price table
            PriceTable(
                prices = foodItem.foodPrices,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PriceTable(
    prices: List<FoodPrice>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "SIZE",
                modifier = Modifier.weight(1f),
                style = ExtendedTypography.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "PRICE",
                modifier = Modifier.weight(1f),
                style = ExtendedTypography.overline,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.End
            )
        }

        // Divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        
        Spacer(modifier = Modifier.height(6.dp))

        // Body
        if (prices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No prices available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(prices) { price ->
                    PriceRow(price = price)
                }
            }
        }
    }
}

@Composable
private fun PriceRow(price: FoodPrice) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isHovered) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) 
        else 
            Color.Transparent,
        animationSpec = tween(AppAnimations.DURATION_FAST)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Size with badge styling
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ) {
            Text(
                text = formatSize(price.foodSize.toString()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        
        // Price with emphasis
        Text(
            text = formatTaka(price.foodPrice),
            style = ExtendedTypography.priceTag,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatSize(size: String): String {
    return size.lowercase().replaceFirstChar { it.uppercase() }
}

private fun formatTaka(value: Double?): String {
    val v = value ?: 0.0
    val df = DecimalFormat("#,##0.##")
    return "৳ ${df.format(v)}"
}
