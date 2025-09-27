package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.FoodItemResponse
import data.network.FoodItemApiService
import ui.components.FoodCard
import ui.viewmodel.FoodItemState

@Composable
fun FoodItemListScreen() {
    // Use shared API (uses HttpClientProvider internally)
    val api = remember { FoodItemApiService() }
    val state = remember { FoodItemState(api) }

    LaunchedEffect(Unit) { state.loadPage(0, 20) }

    Scaffold(
        topBar = {
            TopBar(
                filterText = state.filterText,
                onFilterChange = { state.applyFilter(it) },
                onRefresh = { state.refresh() }
            )
        },
        bottomBar = {
            BottomPagination(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                onPrev = { state.previousPage() },
                onNext = { state.nextPage() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.errorMessage != null -> Text(
                    text = "Error: ${state.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> FoodGrid(items = state.filteredFoodItems())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    filterText: String,
    onFilterChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Text(
                "Food Items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = onFilterChange,
                    singleLine = true,
                    placeholder = { Text("Search by name/serial") },
                    modifier = Modifier
                        .widthIn(min = 180.dp, max = 280.dp)
                        .padding(vertical = 4.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
        }
    )
}

@Composable
private fun FoodGrid(items: List<FoodItemResponse>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth < 400.dp -> 1
            maxWidth < 800.dp -> 2
            else -> 3
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.chunked(columns).forEach { rowItems ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (food in rowItems) {
                            FoodCard(foodItem = food, modifier = Modifier.weight(1f))
                        }
                        repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomPagination(
    currentPage: Int,
    totalPages: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onPrev, enabled = currentPage > 0) { Text("Previous") }
        Text("Page ${currentPage + 1} of $totalPages", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onNext, enabled = currentPage < totalPages - 1) { Text("Next") }
    }
}
