package ui.viewmodel

import androidx.compose.runtime.*
import data.model.*
import data.network.BeverageApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for Beverage Create/Edit screen
 * Manages form state, validation, and API operations
 */
class BeverageEditViewModel(
    private val api: BeverageApiService = BeverageApiService()
) {
    companion object {
        private const val TAG = "BeverageEditViewModel"
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

    // ==================== Error/Success States ====================
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var successMessage by mutableStateOf<String?>(null)
        private set

    // ==================== Form Fields ====================
    
    var beverageName by mutableStateOf("")
    
    // ==================== Current Price Being Built ====================
    
    var currentPrice by mutableStateOf("")
    
    var currentQuantity by mutableStateOf("")
    
    var currentUnit by mutableStateOf<QuantityUnit?>(null)

    // ==================== Beverage Prices List ====================
    
    var beveragePrices by mutableStateOf<List<BeveragePrice>>(emptyList())
        private set

    // ==================== Validation Errors ====================
    
    var nameError by mutableStateOf<String?>(null)
        private set
    
    var duplicateError by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    // ==================== Initialization ====================

    /**
     * Initialize for creating a new beverage
     */
    fun initForCreate() {
        println("[$TAG] initForCreate: Initializing for new beverage")
        isEditMode = false
        editingItemId = null
        resetForm()
    }

    /**
     * Initialize for editing an existing beverage
     */
    fun initForEdit(beverageId: Long) {
        println("[$TAG] initForEdit: Initializing for editing beverage id=$beverageId")
        isEditMode = true
        editingItemId = beverageId
        resetForm()
        loadBeverageForEdit(beverageId)
    }

    /**
     * Load existing beverage data for editing
     */
    private fun loadBeverageForEdit(id: Long) {
        println("[$TAG] loadBeverageForEdit: Loading beverage id=$id")
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val beverage = api.getBeverageById(id)
                println("[$TAG] loadBeverageForEdit: Loaded beverage: ${beverage.name}")
                
                // Populate form fields
                beverageName = beverage.name
                beveragePrices = beverage.prices
                
                println("[$TAG] loadBeverageForEdit: Loaded ${beveragePrices.size} prices")
                
            } catch (e: Exception) {
                println("[$TAG] loadBeverageForEdit: Error - ${e.message}")
                errorMessage = "Failed to load beverage: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // ==================== Form Field Handlers ====================

    fun updateBeverageName(value: String) {
        beverageName = value
        nameError = null
    }

    fun updateCurrentPrice(value: String) {
        // Allow numeric input with decimal
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            currentPrice = value
        }
    }

    fun updateCurrentQuantity(value: String) {
        // Allow numeric input with decimal
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            currentQuantity = value
        }
    }

    fun updateCurrentUnit(unit: QuantityUnit?) {
        currentUnit = unit
    }

    // ==================== Price Management ====================

    /**
     * Add current price to the list
     */
    fun addBeveragePrice() {
        val price = currentPrice.toDoubleOrNull() ?: run {
            println("[$TAG] addBeveragePrice: Invalid price")
            return
        }
        val quantity = currentQuantity.toDoubleOrNull() ?: run {
            println("[$TAG] addBeveragePrice: Invalid quantity")
            return
        }
        val unit = currentUnit ?: run {
            println("[$TAG] addBeveragePrice: No unit selected")
            return
        }

        // Check for duplicate (same quantity and unit)
        val isDuplicate = beveragePrices.any { 
            it.quantity == quantity && it.unit == unit 
        }

        if (isDuplicate) {
            println("[$TAG] addBeveragePrice: Duplicate entry")
            duplicateError = "An entry with the same quantity and unit already exists"
            return
        }

        println("[$TAG] addBeveragePrice: Adding price=$price, quantity=$quantity, unit=$unit")

        val beveragePrice = BeveragePrice(
            price = price,
            quantity = quantity,
            unit = unit
        )

        beveragePrices = beveragePrices + beveragePrice
        duplicateError = null

        // Reset current price fields
        currentPrice = ""
        currentQuantity = ""
        currentUnit = null
    }

    /**
     * Remove a price from the list
     */
    fun removeBeveragePrice(index: Int) {
        println("[$TAG] removeBeveragePrice: Removing price at index=$index")
        beveragePrices = beveragePrices.filterIndexed { i, _ -> i != index }
    }

    /**
     * Check if current price form is valid
     */
    fun canAddPrice(): Boolean {
        return currentPrice.isNotBlank() &&
                currentPrice.toDoubleOrNull() != null &&
                currentQuantity.isNotBlank() &&
                currentQuantity.toDoubleOrNull() != null &&
                currentUnit != null
    }

    // ==================== Validation ====================

    /**
     * Validate form fields
     */
    private fun validate(): Boolean {
        var isValid = true
        
        if (beverageName.isBlank()) {
            nameError = "Beverage name is required"
            isValid = false
        }
        
        if (beveragePrices.isEmpty()) {
            errorMessage = "Please add at least one price"
            isValid = false
        }
        
        println("[$TAG] validate: isValid=$isValid")
        return isValid
    }

    // ==================== Save/Update Operations ====================

    /**
     * Save or update the beverage
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
                val request = BeverageRequest(
                    name = beverageName,
                    prices = beveragePrices
                )
                
                println("[$TAG] save: Request - name=${request.name}, prices=${request.prices.size}")
                
                if (isEditMode && editingItemId != null) {
                    api.updateBeverage(editingItemId!!, request)
                    println("[$TAG] save: Update successful")
                    successMessage = "Beverage updated successfully"
                } else {
                    api.createBeverage(request)
                    println("[$TAG] save: Create successful")
                    successMessage = "Beverage created successfully"
                }
                
                // Invoke success callback
                onSuccess()
                
            } catch (e: Exception) {
                println("[$TAG] save: Error - ${e.message}")
                errorMessage = e.message ?: "Failed to save beverage"
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
        beverageName = ""
        currentPrice = ""
        currentQuantity = ""
        currentUnit = null
        beveragePrices = emptyList()
        nameError = null
        duplicateError = null
        errorMessage = null
        successMessage = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        errorMessage = null
        duplicateError = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        successMessage = null
    }
}


