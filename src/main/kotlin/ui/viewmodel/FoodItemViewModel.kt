package ui.viewmodel

import androidx.compose.runtime.*
import data.model.FoodItemResponse
import data.model.PageFoodItemResponse
import data.network.FoodItemApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodItemState(
    private val api: FoodItemApiService
) {
    // ---------------- State ----------------
    var foodItems by mutableStateOf<List<FoodItemResponse>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var currentPage by mutableStateOf(0)
        private set

    var totalPages by mutableStateOf(1)
        private set

    var filterText by mutableStateOf("")
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    // ---------------- Actions ----------------

    fun loadPage(page: Int = 0, size: Int = 20) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response: PageFoodItemResponse = api.getFoodItems(page, size)
                foodItems = response.content
                currentPage = response.number
                totalPages = response.totalPages
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    fun refresh() {
        loadPage(currentPage)
    }

    fun applyFilter(text: String) {
        filterText = text
    }

    // Returns filtered items (by serial number or name)
    fun filteredFoodItems(): List<FoodItemResponse> {
        if (filterText.isBlank()) return foodItems
        return foodItems.filter {
            it.itemNumber.toString().contains(filterText, ignoreCase = true) ||
                    it.name.contains(filterText, ignoreCase = true)
        }
    }

    fun nextPage() {
        if (currentPage < totalPages - 1) {
            loadPage(currentPage + 1)
        }
    }

    fun previousPage() {
        if (currentPage > 0) {
            loadPage(currentPage - 1)
        }
    }
}
