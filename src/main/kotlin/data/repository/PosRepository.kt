package data.repository

import data.model.*
import data.network.PosApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for POS operations with in-memory caching.
 * Provides reactive state flows for UI consumption.
 */
class PosRepository(
    private val api: PosApiService = PosApiService()
) {
    companion object {
        private const val TAG = "PosRepository"

        @Volatile
        private var INSTANCE: PosRepository? = null

        fun getInstance(): PosRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PosRepository().also { INSTANCE = it }
            }
        }
    }

    private val _ordersCache = MutableStateFlow<Map<Long, FoodOrderShortInfo>>(emptyMap())
    val ordersCache: StateFlow<Map<Long, FoodOrderShortInfo>> = _ordersCache.asStateFlow()

    private val _orderDetailsCache = MutableStateFlow<Map<Long, FoodOrderByCustomer>>(emptyMap())
    val orderDetailsCache: StateFlow<Map<Long, FoodOrderByCustomer>> = _orderDetailsCache.asStateFlow()

    private val _waitersCache = MutableStateFlow<List<WaiterInfo>>(emptyList())
    val waitersCache: StateFlow<List<WaiterInfo>> = _waitersCache.asStateFlow()

    private val _tablesCache = MutableStateFlow<List<TableInfo>>(emptyList())
    val tablesCache: StateFlow<List<TableInfo>> = _tablesCache.asStateFlow()

    private val _foodItemsCache = MutableStateFlow<List<FoodItemShortInfo>>(emptyList())
    val foodItemsCache: StateFlow<List<FoodItemShortInfo>> = _foodItemsCache.asStateFlow()

    private val _beveragesCache = MutableStateFlow<List<BeverageResponse>>(emptyList())
    val beveragesCache: StateFlow<List<BeverageResponse>> = _beveragesCache.asStateFlow()

    private val _paymentMethodsCache = MutableStateFlow<List<PaymentMethodResponse>>(emptyList())
    val paymentMethodsCache: StateFlow<List<PaymentMethodResponse>> = _paymentMethodsCache.asStateFlow()

    private val _printersCache = MutableStateFlow<List<PrinterResponse>>(emptyList())
    val printersCache: StateFlow<List<PrinterResponse>> = _printersCache.asStateFlow()

    private val _systemPrintersCache = MutableStateFlow<List<String>>(emptyList())
    val systemPrintersCache: StateFlow<List<String>> = _systemPrintersCache.asStateFlow()

    private val _defaultPrinter = MutableStateFlow<PrinterResponse?>(null)
    val defaultPrinter: StateFlow<PrinterResponse?> = _defaultPrinter.asStateFlow()

    private val mutex = Mutex()

    suspend fun refreshOrders(): Result<List<FoodOrderShortInfo>> {
        val result = api.getActiveOrders()
        result.onSuccess { orders ->
            mutex.withLock { _ordersCache.value = orders.associateBy { it.id } }
        }
        return result
    }

    suspend fun getOrderDetails(id: Long, forceRefresh: Boolean = false): Result<FoodOrderByCustomer> {
        if (!forceRefresh) {
            _orderDetailsCache.value[id]?.let { cached -> return Result.Success(cached) }
        }
        val result = api.getOrderById(id)
        result.onSuccess { order ->
            mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value + (id to order) }
        }
        return result
    }

    suspend fun createOrder(request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        val result = api.createOrder(request)
        result.onSuccess { order -> mutex.withLock { _ordersCache.value = _ordersCache.value + (order.id to order) } }
        return result
    }

    suspend fun updateOrder(id: Long, request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        val result = api.updateOrder(id, request)
        result.onSuccess { order ->
            mutex.withLock {
                _ordersCache.value = _ordersCache.value + (order.id to order)
                _orderDetailsCache.value = _orderDetailsCache.value - id
            }
        }
        return result
    }

    suspend fun printBill(orderId: Long): Result<FoodOrderByCustomer> {
        val result = api.setPlacedOrBillPrinted(orderId, OrderStatus.BILL_PRINTED)
        result.onSuccess { order -> updateOrderInCacheFromFull(order) }
        refreshOrders()
        return result
    }

    suspend fun markPaid(orderId: Long, paymentMethod: PaymentMethod): Result<FoodOrderByCustomer> {
        val result = api.setPaidOrCancel(orderId, OrderStatus.PAID, paymentMethod)
        result.onSuccess { removeOrderFromCache(orderId) }
        refreshOrders()
        return result
    }

    suspend fun cancelOrder(orderId: Long, reason: String): Result<FoodOrderByCustomer> {
        val result = api.setPaidOrCancel(orderId, OrderStatus.CANCELED, cancelReason = reason)
        result.onSuccess { removeOrderFromCache(orderId) }
        refreshOrders()
        return result
    }

    suspend fun refreshPaymentMethods(): Result<List<PaymentMethodResponse>> {
        val result = api.getPaymentMethods()
        result.onSuccess { paymentMethods ->
            _paymentMethodsCache.value = paymentMethods.sortedWith(
                compareByDescending<PaymentMethodResponse> { it.defaultMethod }.thenBy { it.displayName }
            )
        }
        return result
    }

    suspend fun getAllPaymentMethods(): Result<List<PaymentMethodResponse>> = api.getAllPaymentMethods()

    suspend fun createPaymentMethod(request: PaymentMethodRequest): Result<PaymentMethodResponse> {
        val result = api.createPaymentMethod(request)
        result.onSuccess { refreshPaymentMethods() }
        return result
    }

    suspend fun updatePaymentMethod(id: Long, request: PaymentMethodRequest): Result<PaymentMethodResponse> {
        val result = api.updatePaymentMethod(id, request)
        result.onSuccess { refreshPaymentMethods() }
        return result
    }

    suspend fun setDefaultPaymentMethod(id: Long): Result<PaymentMethodResponse> {
        val result = api.setDefaultPaymentMethod(id)
        result.onSuccess { refreshPaymentMethods() }
        return result
    }

    suspend fun deletePaymentMethod(id: Long): Result<Unit> {
        val result = api.deletePaymentMethod(id)
        result.onSuccess { refreshPaymentMethods() }
        return result
    }

    suspend fun refreshPrinters(): Result<List<PrinterResponse>> {
        val result = api.getPrinters()
        result.onSuccess { printers -> setPrinterCaches(printers) }
        return result
    }

    suspend fun refreshSystemPrinters(): Result<List<String>> {
        val result = api.getSystemPrinters()
        result.onSuccess { printers -> _systemPrintersCache.value = printers }
        return result
    }

    suspend fun getAllPrinters(): Result<List<PrinterResponse>> {
        val result = api.getAllPrinters()
        result.onSuccess { printers -> setPrinterCaches(printers) }
        return result
    }

    suspend fun refreshDefaultPrinter(): Result<PrinterResponse?> {
        val result = api.getDefaultPrinter()
        result.onSuccess { printer -> _defaultPrinter.value = printer }
        return result
    }

    suspend fun createPrinter(request: PrinterRequest): Result<PrinterResponse> {
        val result = api.createPrinter(request)
        result.onSuccess { refreshPrinters() }
        return result
    }

    suspend fun updatePrinter(id: Long, request: PrinterRequest): Result<PrinterResponse> {
        val result = api.updatePrinter(id, request)
        result.onSuccess { refreshPrinters() }
        return result
    }

    suspend fun saveSystemPrinterAsDefault(printerName: String): Result<PrinterResponse> {
        val normalizedName = printerName.trim()
        if (normalizedName.isBlank()) return Result.Error("Select a printer first")

        val existingPrinter = _printersCache.value.firstOrNull {
            it.printerModelName.equals(normalizedName, ignoreCase = true)
        }

        val result = if (existingPrinter != null) {
            if (existingPrinter.active) {
                api.setDefaultPrinter(existingPrinter.id)
            } else {
                api.updatePrinter(
                    existingPrinter.id,
                    PrinterRequest(
                        printerModelName = existingPrinter.printerModelName,
                        defaultPrinter = true,
                        active = true
                    )
                )
            }
        } else {
            api.createPrinter(
                PrinterRequest(
                    printerModelName = normalizedName,
                    defaultPrinter = true,
                    active = true
                )
            )
        }

        result.onSuccess { printer ->
            _defaultPrinter.value = printer
            getAllPrinters()
        }
        return result
    }

    suspend fun setDefaultPrinter(id: Long): Result<PrinterResponse> {
        val result = api.setDefaultPrinter(id)
        result.onSuccess { printer ->
            _defaultPrinter.value = printer
            refreshPrinters()
        }
        return result
    }

    suspend fun deletePrinter(id: Long): Result<Unit> {
        val result = api.deletePrinter(id)
        result.onSuccess { refreshPrinters() }
        return result
    }

    private fun setPrinterCaches(printers: List<PrinterResponse>) {
        val sorted = printers.sortedWith(
            compareByDescending<PrinterResponse> { it.defaultPrinter }.thenBy { it.printerModelName }
        )
        _printersCache.value = sorted
        _defaultPrinter.value = sorted.firstOrNull { it.active && it.defaultPrinter }
    }

    private fun toShortInfo(order: FoodOrderByCustomer): FoodOrderShortInfo {
        return FoodOrderShortInfo(
            id = order.id,
            waiterName = order.waiterName,
            waiterId = order.waiterId,
            totalAmount = order.totalAmount,
            orderStatus = order.orderStatus,
            paymentMethod = order.paymentMethod,
            tableId = order.tableId,
            tableNumber = order.tableNumber,
            createdDateTime = order.createdDateTime
        )
    }

    private suspend fun updateOrderInCacheFromFull(order: FoodOrderByCustomer) {
        mutex.withLock {
            _ordersCache.value = _ordersCache.value + (order.id to toShortInfo(order))
            _orderDetailsCache.value = _orderDetailsCache.value - order.id
        }
    }

    private suspend fun removeOrderFromCache(orderId: Long) {
        mutex.withLock {
            _ordersCache.value = _ordersCache.value - orderId
            _orderDetailsCache.value = _orderDetailsCache.value - orderId
        }
    }

    suspend fun loadLookupData(): Result<Unit> {
        refreshPaymentMethods().onError { error -> println("[$TAG] Failed to load payment methods: ${error.message}") }
        refreshPrinters().onError { error -> println("[$TAG] Failed to load printers: ${error.message}") }
        refreshSystemPrinters().onError { error -> println("[$TAG] Failed to load system printers: ${error.message}") }
        api.getWaiters().onSuccess { _waitersCache.value = it }.onError { error -> println("[$TAG] Failed to load waiters: ${error.message}") }
        api.getTables().onSuccess { _tablesCache.value = it }.onError { error -> println("[$TAG] Failed to load tables: ${error.message}") }
        api.getFoodItemsShortInfo().onSuccess { _foodItemsCache.value = it }.onError { error -> println("[$TAG] Failed to load food items: ${error.message}") }
        api.getBeverages().onSuccess { _beveragesCache.value = it }.onError { error -> println("[$TAG] Failed to load beverages: ${error.message}") }
        return Result.Success(Unit)
    }

    suspend fun loadStartupConfiguration(): Result<Unit> {
        refreshPaymentMethods().onError { error -> println("[$TAG] Failed to load payment methods: ${error.message}") }
        refreshPrinters().onError { error -> println("[$TAG] Failed to load printers: ${error.message}") }
        refreshSystemPrinters().onError { error -> println("[$TAG] Failed to load system printers: ${error.message}") }
        return Result.Success(Unit)
    }

    fun getFoodItemByNumber(itemNumber: Short): FoodItemShortInfo? = _foodItemsCache.value.find { it.itemNumber == itemNumber }
    fun getBeverageById(beverageId: Long): BeverageResponse? = _beveragesCache.value.find { it.id == beverageId }

    fun clearCaches() {
        _ordersCache.value = emptyMap()
        _orderDetailsCache.value = emptyMap()
        _waitersCache.value = emptyList()
        _tablesCache.value = emptyList()
        _foodItemsCache.value = emptyList()
        _beveragesCache.value = emptyList()
        _paymentMethodsCache.value = emptyList()
        _printersCache.value = emptyList()
        _systemPrintersCache.value = emptyList()
        _defaultPrinter.value = null
    }
}
