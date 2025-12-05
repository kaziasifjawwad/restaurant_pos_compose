package ui.viewmodel

import androidx.compose.runtime.*
import data.model.*
import data.network.FoodItemApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Food Item Create/Edit screen
 * Manages form state, validation, and API operations
 */
class FoodItemEditViewModel(
    private val api: FoodItemApiService = FoodItemApiService()
) {
    companion object {
        private const val TAG = "FoodItemEditViewModel"
    }

    // ==================== Screen Mode ====================
    
    var isEditMode by mutableStateOf(false)
        private set
    
    var editingItemId by mutableStateOf<Long?>(null)
        private set

    // ==================== Loading States ====================
    
    var isLoading by mutableStateOf(false)
        private set
    
    var isSaving by mutableStateOf(false)
        private set
    
    var isLoadingLookups by mutableStateOf(false)
        private set

    // ==================== Error/Success States ====================
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var successMessage by mutableStateOf<String?>(null)
        private set

    // ==================== Lookup Data ====================
    
    var allIngredients by mutableStateOf<List<Ingredient>>(emptyList())
        private set
    
    var allCategories by mutableStateOf<List<FoodCategory>>(emptyList())
        private set

    // ==================== Form Fields ====================
    
    var foodName by mutableStateOf("")
    
    var itemNumber by mutableStateOf("")
    
    var description by mutableStateOf("")

    // ==================== Selected Categories ====================
    
    var selectedCategories by mutableStateOf<List<FoodCategory>>(emptyList())
        private set

    // ==================== Current Variant Being Built ====================
    
    var currentFoodSize by mutableStateOf<FoodSize?>(null)
    
    var currentFoodPrice by mutableStateOf("")
    
    var currentIngredient by mutableStateOf<Ingredient?>(null)
    
    var currentIngredientAmount by mutableStateOf("")
    
    var currentUnitOfMeasurement by mutableStateOf<UnitOfMeasurement?>(null)
    
    var currentIngredientAmounts by mutableStateOf<List<IngredientAmountRequest>>(emptyList())
        private set

    // ==================== Food Price Variants ====================
    
    var foodPrices by mutableStateOf<List<FoodPriceRequest>>(emptyList())
        private set

    // ==================== Validation Errors ====================
    
    var nameError by mutableStateOf<String?>(null)
        private set
    
    var itemNumberError by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    // ==================== Initialization ====================

    /**
     * Initialize for creating a new food item
     */
    fun initForCreate() {
        println("[$TAG] initForCreate: Initializing for new food item")
        isEditMode = false
        editingItemId = null
        resetForm()
        loadLookupData()
    }

    /**
     * Initialize for editing an existing food item
     */
    fun initForEdit(foodItemId: Long) {
        println("[$TAG] initForEdit: Initializing for editing food item id=$foodItemId")
        isEditMode = true
        editingItemId = foodItemId
        resetForm()
        loadLookupData()
        loadFoodItemForEdit(foodItemId)
    }

    /**
     * Load lookup data (ingredients and categories)
     */
    private fun loadLookupData() {
        println("[$TAG] loadLookupData: Starting to load ingredients and categories")
        scope.launch {
            isLoadingLookups = true
            errorMessage = null
            
            try {
                // Load ingredients
                println("[$TAG] loadLookupData: Fetching ingredients...")
                allIngredients = api.getIngredients()
                println("[$TAG] loadLookupData: Loaded ${allIngredients.size} ingredients")
                
                // Load categories
                println("[$TAG] loadLookupData: Fetching categories...")
                allCategories = api.getCategories()
                println("[$TAG] loadLookupData: Loaded ${allCategories.size} categories")
                
            } catch (e: Exception) {
                println("[$TAG] loadLookupData: Error - ${e.message}")
                errorMessage = "Failed to load lookup data: ${e.message}"
            } finally {
                isLoadingLookups = false
            }
        }
    }

    /**
     * Load existing food item data for editing
     */
    private fun loadFoodItemForEdit(id: Long) {
        println("[$TAG] loadFoodItemForEdit: Loading food item id=$id")
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val foodItem = api.getFoodItemById(id)
                println("[$TAG] loadFoodItemForEdit: Loaded food item: ${foodItem.name}")
                
                // Populate form fields
                foodName = foodItem.name
                itemNumber = foodItem.itemNumber.toString()
                description = foodItem.description ?: ""
                
                // Populate selected categories
                selectedCategories = foodItem.foodCategories
                println("[$TAG] loadFoodItemForEdit: Loaded ${selectedCategories.size} categories")
                
                // Populate food prices/variants (ensure ingredients are never null)
                foodPrices = foodItem.foodPrices.map { price ->
                    FoodPriceRequest(
                        foodPrice = price.foodPrice,
                        foodSize = price.foodSize,
                        ingredientAmountRequest = price.getIngredients()
                    )
                }
                println("[$TAG] loadFoodItemForEdit: Loaded ${foodPrices.size} variants")
                
            } catch (e: Exception) {
                println("[$TAG] loadFoodItemForEdit: Error - ${e.message}")
                errorMessage = "Failed to load food item: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ==================== Form Field Handlers ====================

    fun updateFoodName(value: String) {
        foodName = value
        nameError = null
    }

    fun updateItemNumber(value: String) {
        // Only allow numeric input
        if (value.isEmpty() || value.all { it.isDigit() }) {
            itemNumber = value
            itemNumberError = null
        }
    }

    fun updateDescription(value: String) {
        description = value
    }

    // ==================== Category Management ====================

    /**
     * Add a category to selected list
     */
    fun addCategory(category: FoodCategory) {
        if (!selectedCategories.any { it.id == category.id }) {
            println("[$TAG] addCategory: Adding category '${category.name}'")
            selectedCategories = selectedCategories + category
        }
    }

    /**
     * Remove a category from selected list
     */
    fun removeCategory(category: FoodCategory) {
        println("[$TAG] removeCategory: Removing category '${category.name}'")
        selectedCategories = selectedCategories.filter { it.id != category.id }
    }

    /**
     * Get available categories (not already selected)
     */
    fun getAvailableCategories(): List<FoodCategory> {
        val selectedIds = selectedCategories.map { it.id }.toSet()
        return allCategories.filter { it.id !in selectedIds }
    }

    // ==================== Ingredient Management ====================

    fun updateCurrentFoodSize(size: FoodSize?) {
        currentFoodSize = size
    }

    fun updateCurrentFoodPrice(value: String) {
        // Allow numeric input with decimal
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            currentFoodPrice = value
        }
    }

    fun updateCurrentIngredient(ingredient: Ingredient?) {
        currentIngredient = ingredient
    }

    fun updateCurrentIngredientAmount(value: String) {
        // Allow numeric input with decimal
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            currentIngredientAmount = value
        }
    }

    fun updateCurrentUnitOfMeasurement(unit: UnitOfMeasurement?) {
        currentUnitOfMeasurement = unit
    }

    /**
     * Add ingredient to current variant's ingredient list
     */
    fun addIngredientToCurrentVariant() {
        val ingredient = currentIngredient ?: return
        val amount = currentIngredientAmount.toDoubleOrNull() ?: return
        val unit = currentUnitOfMeasurement ?: return

        // Check if ingredient already added
        if (currentIngredientAmounts.any { it.ingredientId == ingredient.id }) {
            println("[$TAG] addIngredientToCurrentVariant: Ingredient '${ingredient.name}' already added")
            return
        }

        println("[$TAG] addIngredientToCurrentVariant: Adding '${ingredient.name}' amount=$amount unit=$unit")
        
        val ingredientAmount = IngredientAmountRequest(
            ingredientId = ingredient.id,
            ingredientName = ingredient.name,
            amount = amount,
            unitOfMeasurement = unit
        )
        
        currentIngredientAmounts = currentIngredientAmounts + ingredientAmount
        
        // Reset ingredient fields
        currentIngredient = null
        currentIngredientAmount = ""
        currentUnitOfMeasurement = null
    }

    /**
     * Remove ingredient from current variant
     */
    fun removeIngredientFromCurrentVariant(ingredientId: Long) {
        println("[$TAG] removeIngredientFromCurrentVariant: Removing ingredient id=$ingredientId")
        currentIngredientAmounts = currentIngredientAmounts.filter { it.ingredientId != ingredientId }
    }

    /**
     * Get available ingredients (not already added to current variant)
     */
    fun getAvailableIngredients(): List<Ingredient> {
        val addedIds = currentIngredientAmounts.map { it.ingredientId }.toSet()
        return allIngredients.filter { it.id !in addedIds }
    }

    /**
     * Get available food sizes (not already used in variants)
     */
    fun getAvailableFoodSizes(): List<FoodSize> {
        val usedSizes = foodPrices.map { it.foodSize }.toSet()
        return FoodSize.entries.filter { it !in usedSizes }
    }

    // ==================== Food Variant Management ====================

    /**
     * Add current variant (size + price + ingredients) to food prices list
     */
    fun addFoodVariant() {
        val size = currentFoodSize ?: run {
            println("[$TAG] addFoodVariant: No size selected")
            return
        }
        val price = currentFoodPrice.toDoubleOrNull() ?: run {
            println("[$TAG] addFoodVariant: Invalid price")
            return
        }

        // Check if size already exists
        if (foodPrices.any { it.foodSize == size }) {
            println("[$TAG] addFoodVariant: Size $size already exists")
            errorMessage = "A variant with size '$size' already exists"
            return
        }

        println("[$TAG] addFoodVariant: Adding variant size=$size price=$price ingredients=${currentIngredientAmounts.size}")

        val variant = FoodPriceRequest(
            foodPrice = price,
            foodSize = size,
            ingredientAmountRequest = currentIngredientAmounts.toList()
        )

        foodPrices = foodPrices + variant

        // Reset current variant fields
        currentFoodSize = null
        currentFoodPrice = ""
        currentIngredientAmounts = emptyList()
    }

    /**
     * Remove a food variant
     */
    fun removeFoodVariant(foodSize: FoodSize) {
        println("[$TAG] removeFoodVariant: Removing variant with size=$foodSize")
        foodPrices = foodPrices.filter { it.foodSize != foodSize }
    }

    // ==================== Validation ====================

    /**
     * Validate form fields
     */
    private fun validate(): Boolean {
        var isValid = true
        
        if (foodName.isBlank()) {
            nameError = "Food name is required"
            isValid = false
        }
        
        if (itemNumber.isBlank()) {
            itemNumberError = "Serial number is required"
            isValid = false
        } else if (itemNumber.toIntOrNull() == null) {
            itemNumberError = "Invalid serial number"
            isValid = false
        }
        
        if (selectedCategories.isEmpty()) {
            errorMessage = "Please select at least one category"
            isValid = false
        }
        
        if (foodPrices.isEmpty()) {
            errorMessage = "Please add at least one food variant"
            isValid = false
        }
        
        println("[$TAG] validate: isValid=$isValid")
        return isValid
    }

    // ==================== Save/Update Operations ====================

    /**
     * Save or update the food item
     */
    fun save(onSuccess: () -> Unit) {
        if (!validate()) {
            println("[$TAG] save: Validation failed")
            return
        }

        println("[$TAG] save: Starting ${if (isEditMode) "update" else "create"} operation")
        
        scope.launch {
            isSaving = true
            errorMessage = null
            successMessage = null
            
            try {
                val request = FoodItemRequest(
                    name = foodName,
                    description = description.ifBlank { null },
                    foodCategorySet = selectedCategories.map { it.id },
                    foodPriceSet = foodPrices,
                    itemNumber = itemNumber.toInt()
                )
                
                println("[$TAG] save: Request - name=${request.name}, itemNumber=${request.itemNumber}")
                println("[$TAG] save: Categories=${request.foodCategorySet}, Variants=${request.foodPriceSet.size}")
                
                if (isEditMode && editingItemId != null) {
                    api.updateFoodItem(editingItemId!!, request)
                    println("[$TAG] save: Update successful")
                    successMessage = "Food item updated successfully"
                } else {
                    api.createFoodItem(request)
                    println("[$TAG] save: Create successful")
                    successMessage = "Food item created successfully"
                }
                
                // Invoke success callback
                onSuccess()
                
            } catch (e: Exception) {
                println("[$TAG] save: Error - ${e.message}")
                errorMessage = e.message ?: "Failed to save food item"
            } finally {
                isSaving = false
            }
        }
    }

    // ==================== Utility ====================

    /**
     * Reset all form fields
     */
    private fun resetForm() {
        println("[$TAG] resetForm: Resetting all form fields")
        foodName = ""
        itemNumber = ""
        description = ""
        selectedCategories = emptyList()
        currentFoodSize = null
        currentFoodPrice = ""
        currentIngredient = null
        currentIngredientAmount = ""
        currentUnitOfMeasurement = null
        currentIngredientAmounts = emptyList()
        foodPrices = emptyList()
        nameError = null
        itemNumberError = null
        errorMessage = null
        successMessage = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        successMessage = null
    }

    /**
     * Check if current ingredient form is valid
     */
    fun canAddIngredient(): Boolean {
        return currentIngredient != null &&
                currentIngredientAmount.isNotBlank() &&
                currentIngredientAmount.toDoubleOrNull() != null &&
                currentUnitOfMeasurement != null
    }

    /**
     * Check if current variant form is valid
     */
    fun canAddVariant(): Boolean {
        return currentFoodSize != null &&
                currentFoodPrice.isNotBlank() &&
                currentFoodPrice.toDoubleOrNull() != null
    }
}
