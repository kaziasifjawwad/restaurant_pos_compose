package data.repository

import data.model.*
import data.network.PosApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        
        // Singleton instance
        @Volatile
        private var INSTANCE: PosRepository? = null
        
        fun getInstance(): PosRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PosRepository().also { INSTANCE = it }
            }
        }
    }

    // ==================== State Flows ====================
    
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
    
    // Mutex for thread-safe cache updates
    private val mutex = Mutex()
    
    // ==================== Orders ====================

    /**
     * Refresh orders list from backend and update cache
     */
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

    /**
     * Get order details by ID (with caching)
     */
    suspend fun getOrderDetails(id: Long, forceRefresh: Boolean = false): Result<FoodOrderByCustomer> {
        println("[$TAG] getOrderDetails: id=$id, forceRefresh=$forceRefresh")
        
        // Return from cache if available and not forcing refresh
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

    // ==================== Create/Update ====================

    /**
     * Create a new order and update cache
     */
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

    /**
     * Update an existing order and update cache
     */
    suspend fun updateOrder(id: Long, request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        println("[$TAG] updateOrder: id=$id")
        val result = api.updateOrder(id, request)
        result.onSuccess { order ->
            mutex.withLock {
                _ordersCache.value = _ordersCache.value + (order.id to order)
                // Invalidate details cache for this order
                _orderDetailsCache.value = _orderDetailsCache.value - id
                println("[$TAG] Order updated in cache: id=${order.id}")
            }
        }
        return result
    }

    // ==================== Status Changes ====================

    /**
     * Set order status to BILL_PRINTED
     */
    suspend fun printBill(orderId: Long): Result<FoodOrderShortInfo> {
        println("[$TAG] printBill: orderId=$orderId")
        val result = api.setPlacedOrBillPrinted(orderId, OrderStatus.BILL_PRINTED)
        result.onSuccess { order ->
            updateOrderInCache(order)
        }
        return result
    }

    /**
     * Mark order as PAID
     */
    suspend fun markPaid(orderId: Long): Result<FoodOrderShortInfo> {
        println("[$TAG] markPaid: orderId=$orderId")
        val result = api.setPaidOrCancel(orderId, OrderStatus.PAID)
        result.onSuccess { order ->
            // Remove from active orders cache (paid orders are no longer active)
            removeOrderFromCache(orderId)
        }
        return result
    }

    /**
     * Cancel an order
     */
    suspend fun cancelOrder(orderId: Long): Result<FoodOrderShortInfo> {
        println("[$TAG] cancelOrder: orderId=$orderId")
        val result = api.setPaidOrCancel(orderId, OrderStatus.CANCELED)
        result.onSuccess { order ->
            // Remove from active orders cache
            removeOrderFromCache(orderId)
        }
        return result
    }

    private suspend fun updateOrderInCache(order: FoodOrderShortInfo) {
        mutex.withLock {
            _ordersCache.value = _ordersCache.value + (order.id to order)
            // Invalidate details cache
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

    // ==================== Lookup Data ====================

    /**
     * Load all lookup data (waiters, tables, food items, beverages)
     */
    suspend fun loadLookupData(): Result<Unit> {
        println("[$TAG] loadLookupData")
        
        // Load waiters
        api.getWaiters().onSuccess { waiters ->
            _waitersCache.value = waiters
            println("[$TAG] Waiters loaded: ${waiters.size}")
        }.onError { error ->
            println("[$TAG] Failed to load waiters: ${error.message}")
        }
        
        // Load tables
        api.getTables().onSuccess { tables ->
            _tablesCache.value = tables
            println("[$TAG] Tables loaded: ${tables.size}")
        }.onError { error ->
            println("[$TAG] Failed to load tables: ${error.message}")
        }
        
        // Load food items
        api.getFoodItemsShortInfo().onSuccess { items ->
            _foodItemsCache.value = items
            println("[$TAG] Food items loaded: ${items.size}")
        }.onError { error ->
            println("[$TAG] Failed to load food items: ${error.message}")
        }
        
        // Load beverages
        api.getBeverages().onSuccess { beverages ->
            _beveragesCache.value = beverages
            println("[$TAG] Beverages loaded: ${beverages.size}")
        }.onError { error ->
            println("[$TAG] Failed to load beverages: ${error.message}")
        }
        
        return Result.Success(Unit)
    }

    /**
     * Get food item by item number
     */
    fun getFoodItemByNumber(itemNumber: Short): FoodItemShortInfo? {
        return _foodItemsCache.value.find { it.itemNumber == itemNumber }
    }

    /**
     * Get beverage by ID
     */
    fun getBeverageById(beverageId: Long): BeverageResponse? {
        return _beveragesCache.value.find { it.id == beverageId }
    }

    // ==================== Clear ====================

    /**
     * Clear all caches (e.g., on logout)
     */
    fun clearCaches() {
        println("[$TAG] clearCaches")
        _ordersCache.value = emptyMap()
        _orderDetailsCache.value = emptyMap()
        _waitersCache.value = emptyList()
        _tablesCache.value = emptyList()
        _foodItemsCache.value = emptyList()
        _beveragesCache.value = emptyList()
    }
}
