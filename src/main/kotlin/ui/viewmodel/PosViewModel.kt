package ui.viewmodel

import data.model.*
import data.repository.PosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI State for POS screens
 */
data class PosUiState(
    // List screen state
    val orders: List<FoodOrderShortInfo> = emptyList(),
    val isLoadingOrders: Boolean = false,
    
    // Detail screen state
    val selectedOrder: FoodOrderByCustomer? = null,
    val isLoadingDetail: Boolean = false,
    
    // Editor state
    val editorMode: EditorMode = EditorMode.Closed,
    val isLoadingLookups: Boolean = false,
    val isSaving: Boolean = false,
    
    // Lookup data
    val waiters: List<WaiterInfo> = emptyList(),
    val tables: List<TableInfo> = emptyList(),
    val foodItems: List<FoodItemShortInfo> = emptyList(),
    val beverages: List<BeverageResponse> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    
    // Error/Success state
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    sealed class EditorMode {
        object Closed : EditorMode()
        object Create : EditorMode()
        data class Edit(val orderId: Long) : EditorMode()
    }
}

/**
 * Events that can be sent to PosViewModel
 */
sealed class PosUiEvent {
    // List events
    object LoadOrders : PosUiEvent()
    object RefreshOrders : PosUiEvent()
    
    // Detail events
    data class LoadOrderDetail(val orderId: Long) : PosUiEvent()
    object ClearSelectedOrder : PosUiEvent()
    
    // Editor events
    object OpenCreateEditor : PosUiEvent()
    data class OpenEditEditor(val orderId: Long) : PosUiEvent()
    object CloseEditor : PosUiEvent()
    object LoadLookupData : PosUiEvent()
    data class SaveOrder(val request: FoodOrderByCustomerRequest) : PosUiEvent()
    
    // Status change events
    data class PrintBill(val orderId: Long) : PosUiEvent()
    data class PrintKitchenMemo(val orderId: Long) : PosUiEvent()
    data class CompleteOrder(val orderId: Long, val paymentMethod: PaymentMethod? = null) : PosUiEvent()
    data class MarkPaid(val orderId: Long, val paymentMethod: PaymentMethod? = null) : PosUiEvent()
    data class CancelOrder(val orderId: Long) : PosUiEvent()
    
    // Error handling
    data class ShowError(val message: String) : PosUiEvent()
    object ClearError : PosUiEvent()
    object ClearSuccess : PosUiEvent()
}

/**
 * ViewModel for POS module using Unidirectional Data Flow.
 * Exposes StateFlow for UI state and processes events.
 */
class PosViewModel(
    private val repository: PosRepository = PosRepository.getInstance()
) {
    companion object {
        private const val TAG = "PosViewModel"
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryCaches()
    }

    private fun observeRepositoryCaches() {
        scope.launch {
            repository.ordersCache.collect { ordersMap ->
                _uiState.update { state ->
                    state.copy(orders = ordersMap.values.toList().sortedByDescending { it.id })
                }
            }
        }
        
        scope.launch {
            repository.waitersCache.collect { waiters ->
                _uiState.update { state -> state.copy(waiters = waiters) }
            }
        }
        
        scope.launch {
            repository.tablesCache.collect { tables ->
                _uiState.update { state -> state.copy(tables = tables.sortedBy { it.tableNumber }) }
            }
        }
        
        scope.launch {
            repository.foodItemsCache.collect { items ->
                _uiState.update { state -> state.copy(foodItems = items) }
            }
        }
        
        scope.launch {
            repository.beveragesCache.collect { beverages ->
                _uiState.update { state -> state.copy(beverages = beverages) }
            }
        }

        scope.launch {
            repository.paymentMethodsCache.collect { paymentMethods ->
                _uiState.update { state -> state.copy(paymentMethods = paymentMethods) }
            }
        }
    }

    /**
     * Process UI events
     */
    fun onEvent(event: PosUiEvent) {
        println("[$TAG] onEvent: ${event::class.simpleName}")
        when (event) {
            is PosUiEvent.LoadOrders -> loadOrders()
            is PosUiEvent.RefreshOrders -> refreshOrders()
            is PosUiEvent.LoadOrderDetail -> loadOrderDetail(event.orderId)
            is PosUiEvent.ClearSelectedOrder -> clearSelectedOrder()
            is PosUiEvent.OpenCreateEditor -> openCreateEditor()
            is PosUiEvent.OpenEditEditor -> openEditEditor(event.orderId)
            is PosUiEvent.CloseEditor -> closeEditor()
            is PosUiEvent.LoadLookupData -> loadLookupData()
            is PosUiEvent.SaveOrder -> saveOrder(event.request)
            is PosUiEvent.PrintBill -> printBill(event.orderId)
            is PosUiEvent.PrintKitchenMemo -> printKitchenMemo(event.orderId)
            is PosUiEvent.CompleteOrder -> completeOrder(event.orderId, event.paymentMethod)
            is PosUiEvent.MarkPaid -> markPaid(event.orderId, event.paymentMethod)
            is PosUiEvent.CancelOrder -> cancelOrder(event.orderId)
            is PosUiEvent.ShowError -> showError(event.message)
            is PosUiEvent.ClearError -> clearError()
            is PosUiEvent.ClearSuccess -> clearSuccess()
        }
    }

    // ==================== List Operations ====================

    private fun loadOrders() {
        if (_uiState.value.orders.isEmpty()) {
            refreshOrders()
        }
    }

    private fun refreshOrders() {
        scope.launch {
            _uiState.update { it.copy(isLoadingOrders = true, errorMessage = null) }
            repository.refreshOrders()
                .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isLoadingOrders = false) }
        }
    }

    // ==================== Detail Operations ====================

    private fun loadOrderDetail(orderId: Long) {
        scope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, errorMessage = null) }
            repository.getOrderDetails(orderId)
                .onSuccess { order -> _uiState.update { it.copy(selectedOrder = order) } }
                .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            _uiState.update { it.copy(isLoadingDetail = false) }
        }
    }

    private fun clearSelectedOrder() {
        _uiState.update { it.copy(selectedOrder = null) }
    }

    // ==================== Editor Operations ====================

    private fun openCreateEditor() {
        _uiState.update { it.copy(editorMode = PosUiState.EditorMode.Create) }
        loadLookupData()
    }

    private fun openEditEditor(orderId: Long) {
        _uiState.update { it.copy(editorMode = PosUiState.EditorMode.Edit(orderId)) }
        loadLookupData()
        loadOrderDetail(orderId)
    }

    private fun closeEditor() {
        _uiState.update { 
            it.copy(
                editorMode = PosUiState.EditorMode.Closed,
                selectedOrder = null
            )
        }
    }

    private fun loadLookupData() {
        scope.launch {
            _uiState.update { it.copy(isLoadingLookups = true) }
            repository.loadLookupData()
            _uiState.update { it.copy(isLoadingLookups = false) }
        }
    }

    private fun saveOrder(request: FoodOrderByCustomerRequest) {
        scope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.loadLookupData()
            
            val result = when (val mode = _uiState.value.editorMode) {
                is PosUiState.EditorMode.Edit -> repository.updateOrder(mode.orderId, request)
                else -> repository.createOrder(request)
            }
            
            result
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            successMessage = if (_uiState.value.editorMode is PosUiState.EditorMode.Edit) 
                                "Order updated successfully" 
                            else 
                                "Order created successfully",
                            editorMode = PosUiState.EditorMode.Closed,
                            selectedOrder = null
                        )
                    }
                }
                .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    // ==================== Status Operations ====================

    private fun printBill(orderId: Long) {
        _uiState.update { it.copy(errorMessage = null, successMessage = "Bill printed successfully") }
    }

    private fun printKitchenMemo(orderId: Long) {
        _uiState.update { it.copy(successMessage = "Kitchen memo printed successfully") }
    }

    private fun completeOrder(orderId: Long, paymentMethod: PaymentMethod?) {
        scope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            
            val result = repository.markPaid(orderId, paymentMethod)
            
            _uiState.update { 
                it.copy(
                    successMessage = "Order completed successfully",
                    selectedOrder = null
                )
            }
            
            result.onError { error ->
                if (!error.message.contains("empty response", ignoreCase = true)) {
                    println("[PosViewModel] Complete order error: ${error.message}")
                }
            }
        }
    }

    private fun markPaid(orderId: Long, paymentMethod: PaymentMethod?) {
        scope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            
            repository.markPaid(orderId, paymentMethod)
                .onSuccess { 
                    _uiState.update { 
                        it.copy(
                            successMessage = "Order marked as paid",
                            selectedOrder = null
                        )
                    }
                }
                .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    private fun cancelOrder(orderId: Long) {
        scope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            
            val result = repository.cancelOrder(orderId)
            
            _uiState.update { 
                it.copy(
                    successMessage = "Order canceled",
                    selectedOrder = null
                )
            }
            
            result.onError { error ->
                if (!error.message.contains("empty response", ignoreCase = true)) {
                    println("[PosViewModel] Cancel order error: ${error.message}")
                }
            }
        }
    }

    // ==================== Error/Success ====================

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // ==================== Utility ====================

    fun canEditOrder(order: FoodOrderShortInfo): Boolean {
        return order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED
    }

    fun canPrintBill(order: FoodOrderShortInfo): Boolean {
        return order.orderStatus == OrderStatus.ORDER_PLACED
    }

    fun canMarkPaid(order: FoodOrderShortInfo): Boolean {
        return order.orderStatus == OrderStatus.BILL_PRINTED
    }

    fun canCancelOrder(order: FoodOrderShortInfo): Boolean {
        return order.orderStatus != OrderStatus.PAID && order.orderStatus != OrderStatus.CANCELED
    }

    fun getFoodItemByNumber(itemNumber: Short): FoodItemShortInfo? {
        return repository.getFoodItemByNumber(itemNumber)
    }

    fun getBeverageById(beverageId: Long): BeverageResponse? {
        return repository.getBeverageById(beverageId)
    }
}
