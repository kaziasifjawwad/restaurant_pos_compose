package ui.viewmodel

import data.model.*
import data.print.PosPrinterService
import data.repository.PosRepository
import data.repository.TakeoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TakeoutUiState(
    val activeOrders: List<TakeoutOrderShortInfo> = emptyList(),
    val selectedOrder: TakeoutOrderResponse? = null,
    val mediums: List<TakeoutMediumResponse> = emptyList(),
    val paymentMethods: List<PaymentMethodResponse> = emptyList(),
    val foodItems: List<FoodItemShortInfo> = emptyList(),
    val beverages: List<BeverageResponse> = emptyList(),
    val isLoadingOrders: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isLoadingLookups: Boolean = false,
    val isSaving: Boolean = false,
    val editorMode: EditorMode = EditorMode.Closed,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    sealed class EditorMode {
        object Closed : EditorMode()
        object Create : EditorMode()
        data class Edit(val orderId: Long) : EditorMode()
    }
}

sealed class TakeoutUiEvent {
    object LoadActiveOrders : TakeoutUiEvent()
    object RefreshActiveOrders : TakeoutUiEvent()
    data class LoadOrderDetail(val orderId: Long) : TakeoutUiEvent()
    object ClearSelectedOrder : TakeoutUiEvent()
    object OpenCreateEditor : TakeoutUiEvent()
    data class OpenEditEditor(val orderId: Long) : TakeoutUiEvent()
    object CloseEditor : TakeoutUiEvent()
    object LoadLookupData : TakeoutUiEvent()
    object LoadAllMediums : TakeoutUiEvent()
    data class SaveOrder(val request: TakeoutOrderRequest) : TakeoutUiEvent()
    data class UpdateStatus(val orderId: Long, val status: TakeoutOrderStatus) : TakeoutUiEvent()
    data class UpdatePayment(val orderId: Long, val request: TakeoutPaymentUpdateRequest) : TakeoutUiEvent()
    data class CancelOrder(val orderId: Long, val reason: String) : TakeoutUiEvent()
    data class SaveMedium(val id: Long?, val request: TakeoutMediumRequest) : TakeoutUiEvent()
    data class DeleteMedium(val id: Long) : TakeoutUiEvent()
    data class ShowError(val message: String) : TakeoutUiEvent()
    object ClearError : TakeoutUiEvent()
    object ClearSuccess : TakeoutUiEvent()
}

class TakeoutViewModel(
    private val repository: TakeoutRepository = TakeoutRepository.instance,
    private val posRepository: PosRepository = PosRepository.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _uiState = MutableStateFlow(TakeoutUiState())
    val uiState: StateFlow<TakeoutUiState> = _uiState.asStateFlow()

    init { observeCaches() }

    private fun observeCaches() {
        scope.launch { repository.activeOrdersCache.collect { cache -> _uiState.update { it.copy(activeOrders = cache.values.sortedByDescending { order -> order.id }) } } }
        scope.launch { repository.mediumsCache.collect { mediums -> _uiState.update { it.copy(mediums = mediums) } } }
        scope.launch { repository.paymentMethodsCache.collect { methods -> _uiState.update { it.copy(paymentMethods = methods) } } }
        scope.launch { repository.foodItemsCache.collect { items -> _uiState.update { it.copy(foodItems = items) } } }
        scope.launch { repository.beveragesCache.collect { beverages -> _uiState.update { it.copy(beverages = beverages) } } }
    }

    fun onEvent(event: TakeoutUiEvent) {
        when (event) {
            TakeoutUiEvent.LoadActiveOrders -> if (_uiState.value.activeOrders.isEmpty()) refreshActiveOrders()
            TakeoutUiEvent.RefreshActiveOrders -> refreshActiveOrders()
            is TakeoutUiEvent.LoadOrderDetail -> loadOrderDetail(event.orderId)
            TakeoutUiEvent.ClearSelectedOrder -> _uiState.update { it.copy(selectedOrder = null) }
            TakeoutUiEvent.OpenCreateEditor -> openCreateEditor()
            is TakeoutUiEvent.OpenEditEditor -> openEditEditor(event.orderId)
            TakeoutUiEvent.CloseEditor -> _uiState.update { it.copy(editorMode = TakeoutUiState.EditorMode.Closed, selectedOrder = null) }
            TakeoutUiEvent.LoadLookupData -> loadLookupData()
            TakeoutUiEvent.LoadAllMediums -> loadAllMediums()
            is TakeoutUiEvent.SaveOrder -> saveOrder(event.request)
            is TakeoutUiEvent.UpdateStatus -> updateStatus(event.orderId, event.status)
            is TakeoutUiEvent.UpdatePayment -> updatePayment(event.orderId, event.request)
            is TakeoutUiEvent.CancelOrder -> cancelOrder(event.orderId, event.reason)
            is TakeoutUiEvent.SaveMedium -> saveMedium(event.id, event.request)
            is TakeoutUiEvent.DeleteMedium -> deleteMedium(event.id)
            is TakeoutUiEvent.ShowError -> _uiState.update { it.copy(errorMessage = event.message) }
            TakeoutUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            TakeoutUiEvent.ClearSuccess -> _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun refreshActiveOrders() = scope.launch {
        _uiState.update { it.copy(isLoadingOrders = true, errorMessage = null) }
        repository.refreshActiveOrders().onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isLoadingOrders = false) }
    }

    private fun loadLookupData() = scope.launch {
        _uiState.update { it.copy(isLoadingLookups = true) }
        repository.loadLookupData().onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isLoadingLookups = false) }
    }

    private fun loadAllMediums() = scope.launch {
        _uiState.update { it.copy(isLoadingLookups = true, errorMessage = null) }
        repository.getAllMediums().onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isLoadingLookups = false) }
    }

    private fun loadOrderDetail(orderId: Long) = scope.launch {
        _uiState.update { it.copy(isLoadingDetail = true, errorMessage = null) }
        repository.getOrderDetails(orderId, true)
            .onSuccess { order -> _uiState.update { it.copy(selectedOrder = order) } }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isLoadingDetail = false) }
    }

    private fun openCreateEditor() {
        _uiState.update { it.copy(editorMode = TakeoutUiState.EditorMode.Create) }
        loadLookupData()
    }

    private fun openEditEditor(orderId: Long) {
        _uiState.update { it.copy(editorMode = TakeoutUiState.EditorMode.Edit(orderId)) }
        loadLookupData()
        loadOrderDetail(orderId)
    }

    private fun saveOrder(request: TakeoutOrderRequest) = scope.launch {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val isEditMode = _uiState.value.editorMode is TakeoutUiState.EditorMode.Edit
        val result = when (val mode = _uiState.value.editorMode) {
            is TakeoutUiState.EditorMode.Edit -> repository.updateOrder(mode.orderId, request)
            else -> repository.createOrder(request)
        }
        result.onSuccess { savedOrder ->
            val printMessage = if (!isEditMode) printKitchenMemoForNewOrder(savedOrder.id) else null
            _uiState.update {
                it.copy(
                    successMessage = when {
                        isEditMode -> "Takeout order saved"
                        printMessage != null -> "Takeout order saved. $printMessage"
                        else -> "Takeout order saved and kitchen memo printed"
                    },
                    editorMode = TakeoutUiState.EditorMode.Closed
                )
            }
        }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        _uiState.update { it.copy(isSaving = false) }
    }

    private fun updateStatus(orderId: Long, status: TakeoutOrderStatus) = scope.launch {
        val printer = if (status == TakeoutOrderStatus.COMPLETED) ensureDefaultPrinter() else null
        repository.updateStatus(orderId, TakeoutStatusUpdateRequest(status))
            .onSuccess { order ->
                val printMessage = if (status == TakeoutOrderStatus.COMPLETED) printCashMemoAfterCompleted(order, printer) else null
                _uiState.update {
                    it.copy(
                        successMessage = when {
                            status == TakeoutOrderStatus.COMPLETED && printMessage == null -> "Status updated and cash memo printed"
                            status == TakeoutOrderStatus.COMPLETED -> "Status updated. $printMessage"
                            else -> "Status updated"
                        },
                        selectedOrder = order
                    )
                }
            }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
    }

    private suspend fun printKitchenMemoForNewOrder(orderId: Long): String? {
        val printer = ensureDefaultPrinter()
            ?: return "Kitchen memo was not printed because no default printer is configured."

        val order = when (val detailsResult = repository.getOrderDetails(orderId, forceRefresh = true)) {
            is Result.Success -> detailsResult.data
            is Result.Error -> return "Kitchen memo was not printed: ${detailsResult.message}"
        }

        return PosPrinterService.printTakeoutKitchenMemo(order, printer.printerModelName)
            .fold(
                onSuccess = { null },
                onFailure = { error -> "Kitchen memo was not printed: ${error.message ?: "printer error"}" }
            )
    }

    private suspend fun ensureDefaultPrinter(): PrinterResponse? {
        posRepository.refreshPrinters()
        val printer = posRepository.defaultPrinter.value
        if (printer == null) {
            _uiState.update { it.copy(errorMessage = "No default printer configured. Please configure a printer first.") }
        }
        return printer
    }

    private fun printCashMemoAfterCompleted(order: TakeoutOrderResponse, printer: PrinterResponse?): String? {
        if (printer == null) return "Cash memo was not printed because no default printer is configured."
        return PosPrinterService.printTakeoutCashMemo(order, printer.printerModelName)
            .fold(
                onSuccess = { null },
                onFailure = { error -> "Cash memo was not printed: ${error.message ?: "printer error"}" }
            )
    }

    private fun updatePayment(orderId: Long, request: TakeoutPaymentUpdateRequest) = scope.launch {
        repository.updatePayment(orderId, request)
            .onSuccess { order -> _uiState.update { it.copy(successMessage = "Payment updated", selectedOrder = order) } }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
    }

    private fun cancelOrder(orderId: Long, reason: String) = scope.launch {
        repository.cancelOrder(orderId, reason)
            .onSuccess { order -> _uiState.update { it.copy(successMessage = "Takeout order canceled", selectedOrder = order) } }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
    }

    private fun saveMedium(id: Long?, request: TakeoutMediumRequest) = scope.launch {
        val result = if (id == null) repository.createMedium(request) else repository.updateMedium(id, request)
        result.onSuccess { _uiState.update { it.copy(successMessage = "Medium saved") } }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
    }

    private fun deleteMedium(id: Long) = scope.launch {
        repository.deleteMedium(id)
            .onSuccess { _uiState.update { it.copy(successMessage = "Medium deactivated") } }
            .onError { error -> _uiState.update { it.copy(errorMessage = error.message) } }
    }

    fun canEditOrder(order: TakeoutOrderShortInfo): Boolean = !order.orderStatus.isFinal
    fun canCancelOrder(order: TakeoutOrderShortInfo): Boolean = order.orderStatus in setOf(TakeoutOrderStatus.ORDER_RECEIVED, TakeoutOrderStatus.ACCEPTED, TakeoutOrderStatus.PREPARING, TakeoutOrderStatus.READY_FOR_PICKUP)
    fun canUpdatePayment(order: TakeoutOrderShortInfo): Boolean = !order.orderStatus.isFinal
}
