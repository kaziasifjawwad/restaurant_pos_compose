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

    private val mutex = Mutex()

    suspend fun refreshOrders(): Result<List<FoodOrderShortInfo>> {
        println("[$TAG] refreshOrders")
        val result = api.getActiveOrders()
        result.onSuccess { orders ->
            mutex.withLock {
                val newMap = orders.associateBy { it.id }
                _ordersCache.value = newMap
                println("[$TAG] Orders cache updated: ${newMap.size} orders")
            }
        }
        return result
    }

    suspend fun getOrderDetails(id: Long, forceRefresh: Boolean = false): Result<FoodOrderByCustomer> {
        println("[$TAG] getOrderDetails: id=$id, forceRefresh=$forceRefresh")
        if (!forceRefresh) {
            _orderDetailsCache.value[id]?.let { cached ->
                println("[$TAG] Returning cached order details")
                return Result.Success(cached)
            }
        }

        val result = api.getOrderById(id)
        result.onSuccess { order ->
            mutex.withLock {
                _orderDetailsCache.value = _orderDetailsCache.value + (id to order)
                println("[$TAG] Order details cached: id=$id")
            }
        }
        return result
    }

    suspend fun createOrder(request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        println("[$TAG] createOrder")
        val result = api.createOrder(request)
        result.onSuccess { order ->
            mutex.withLock {
                _ordersCache.value = _ordersCache.value + (order.id to order)
                println("[$TAG] New order added to cache: id=${order.id}")
            }
        }
        return result
    }

    suspend fun updateOrder(id: Long, request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        println("[$TAG] updateOrder: id=$id")
        val result = api.updateOrder(id, request)
        result.onSuccess { order ->
            mutex.withLock {
                _ordersCache.value = _ordersCache.value + (order.id to order)
                _orderDetailsCache.value = _orderDetailsCache.value - id
                println("[$TAG] Order updated in cache: id=${order.id}")
            }
        }
        return result
    }

    suspend fun printBill(orderId: Long): Result<FoodOrderByCustomer> {
        println("[$TAG] printBill: orderId=$orderId")
        val result = api.setPlacedOrBillPrinted(orderId, OrderStatus.BILL_PRINTED)
        result.onSuccess { order -> updateOrderInCacheFromFull(order) }
        refreshOrders()
        return result
    }

    suspend fun markPaid(orderId: Long, paymentMethod: PaymentMethod): Result<FoodOrderByCustomer> {
        println("[$TAG] markPaid: orderId=$orderId, paymentMethod=$paymentMethod")
        val result = api.setPaidOrCancel(orderId, OrderStatus.PAID, paymentMethod)
        result.onSuccess { removeOrderFromCache(orderId) }
        refreshOrders()
        return result
    }

    suspend fun cancelOrder(orderId: Long): Result<FoodOrderByCustomer> {
        println("[$TAG] cancelOrder: orderId=$orderId")
        val result = api.setPaidOrCancel(orderId, OrderStatus.CANCELED)
        result.onSuccess { removeOrderFromCache(orderId) }
        refreshOrders()
        return result
    }

    suspend fun refreshPaymentMethods(): Result<List<PaymentMethodResponse>> {
        println("[$TAG] refreshPaymentMethods")
        val result = api.getPaymentMethods()
        result.onSuccess { paymentMethods ->
            _paymentMethodsCache.value = paymentMethods.sortedWith(
                compareByDescending<PaymentMethodResponse> { it.defaultMethod }.thenBy { it.displayName }
            )
            println("[$TAG] Payment methods loaded: ${paymentMethods.size}")
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
            val shortInfo = toShortInfo(order)
            _ordersCache.value = _ordersCache.value + (order.id to shortInfo)
            _orderDetailsCache.value = _orderDetailsCache.value - order.id
            println("[$TAG] Order status updated in cache: id=${order.id}, status=${order.orderStatus}")
        }
    }

    private suspend fun removeOrderFromCache(orderId: Long) {
        mutex.withLock {
            _ordersCache.value = _ordersCache.value - orderId
            _orderDetailsCache.value = _orderDetailsCache.value - orderId
            println("[$TAG] Order removed from cache: id=$orderId")
        }
    }

    suspend fun loadLookupData(): Result<Unit> {
        println("[$TAG] loadLookupData")

        refreshPaymentMethods().onError { error ->
            println("[$TAG] Failed to load payment methods: ${error.message}")
        }

        api.getWaiters().onSuccess { waiters ->
            _waitersCache.value = waiters
            println("[$TAG] Waiters loaded: ${waiters.size}")
        }.onError { error -> println("[$TAG] Failed to load waiters: ${error.message}") }

        api.getTables().onSuccess { tables ->
            _tablesCache.value = tables
            println("[$TAG] Tables loaded: ${tables.size}")
        }.onError { error -> println("[$TAG] Failed to load tables: ${error.message}") }

        api.getFoodItemsShortInfo().onSuccess { items ->
            _foodItemsCache.value = items
            println("[$TAG] Food items loaded: ${items.size}")
        }.onError { error -> println("[$TAG] Failed to load food items: ${error.message}") }

        api.getBeverages().onSuccess { beverages ->
            _beveragesCache.value = beverages
            println("[$TAG] Beverages loaded: ${beverages.size}")
        }.onError { error -> println("[$TAG] Failed to load beverages: ${error.message}") }

        return Result.Success(Unit)
    }

    fun getFoodItemByNumber(itemNumber: Short): FoodItemShortInfo? {
        return _foodItemsCache.value.find { it.itemNumber == itemNumber }
    }

    fun getBeverageById(beverageId: Long): BeverageResponse? {
        return _beveragesCache.value.find { it.id == beverageId }
    }

    fun clearCaches() {
        println("[$TAG] clearCaches")
        _ordersCache.value = emptyMap()
        _orderDetailsCache.value = emptyMap()
        _waitersCache.value = emptyList()
        _tablesCache.value = emptyList()
        _foodItemsCache.value = emptyList()
        _beveragesCache.value = emptyList()
        _paymentMethodsCache.value = emptyList()
    }
}
