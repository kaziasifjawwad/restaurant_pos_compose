package data.repository

import data.model.*
import data.network.TakeoutApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TakeoutRepository private constructor(
    private val apiService: TakeoutApiService = TakeoutApiService()
) {
    private val mutex = Mutex()

    private val _activeOrdersCache = MutableStateFlow<Map<Long, TakeoutOrderShortInfo>>(emptyMap())
    val activeOrdersCache: StateFlow<Map<Long, TakeoutOrderShortInfo>> = _activeOrdersCache

    private val _orderDetailsCache = MutableStateFlow<Map<Long, TakeoutOrderResponse>>(emptyMap())
    val orderDetailsCache: StateFlow<Map<Long, TakeoutOrderResponse>> = _orderDetailsCache

    private val _mediumsCache = MutableStateFlow<List<TakeoutMediumResponse>>(emptyList())
    val mediumsCache: StateFlow<List<TakeoutMediumResponse>> = _mediumsCache

    private val _paymentMethodsCache = MutableStateFlow<List<PaymentMethodResponse>>(emptyList())
    val paymentMethodsCache: StateFlow<List<PaymentMethodResponse>> = _paymentMethodsCache

    private val _foodItemsCache = MutableStateFlow<List<FoodItemShortInfo>>(emptyList())
    val foodItemsCache: StateFlow<List<FoodItemShortInfo>> = _foodItemsCache

    private val _beveragesCache = MutableStateFlow<List<BeverageResponse>>(emptyList())
    val beveragesCache: StateFlow<List<BeverageResponse>> = _beveragesCache

    suspend fun refreshActiveOrders(): Result<List<TakeoutOrderShortInfo>> {
        return when (val result = apiService.getActiveTakeoutOrders()) {
            is Result.Success -> {
                mutex.withLock { _activeOrdersCache.value = result.data.associateBy { it.id } }
                result
            }
            is Result.Error -> result
        }
    }

    suspend fun searchOrders(page: Int = 0, size: Int = 20): Result<PageTakeoutOrderShortInfoResponse> =
        apiService.getTakeoutOrders(page, size)

    suspend fun getOrderDetails(id: Long, forceRefresh: Boolean = false): Result<TakeoutOrderResponse> {
        if (!forceRefresh) {
            _orderDetailsCache.value[id]?.let { return Result.Success(it) }
        }
        return when (val result = apiService.getTakeoutOrderById(id)) {
            is Result.Success -> {
                mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value + (id to result.data) }
                result
            }
            is Result.Error -> result
        }
    }

    suspend fun createOrder(request: TakeoutOrderRequest): Result<TakeoutOrderShortInfo> {
        val result = apiService.createTakeoutOrder(request)
        if (result is Result.Success) refreshActiveOrders()
        return result
    }

    suspend fun updateOrder(id: Long, request: TakeoutOrderRequest): Result<TakeoutOrderShortInfo> {
        val result = apiService.updateTakeoutOrder(id, request)
        if (result is Result.Success) {
            mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value - id }
            refreshActiveOrders()
        }
        return result
    }

    suspend fun updateStatus(id: Long, request: TakeoutStatusUpdateRequest): Result<TakeoutOrderResponse> {
        val result = apiService.updateTakeoutStatus(id, request)
        if (result is Result.Success) {
            mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value + (id to result.data) }
            refreshActiveOrders()
        }
        return result
    }

    suspend fun updatePayment(id: Long, request: TakeoutPaymentUpdateRequest): Result<TakeoutOrderResponse> {
        val result = apiService.updateTakeoutPayment(id, request)
        if (result is Result.Success) {
            mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value + (id to result.data) }
            refreshActiveOrders()
        }
        return result
    }

    suspend fun cancelOrder(id: Long, reason: String): Result<TakeoutOrderResponse> {
        val result = apiService.cancelTakeoutOrder(id, reason)
        if (result is Result.Success) {
            mutex.withLock { _orderDetailsCache.value = _orderDetailsCache.value + (id to result.data) }
            refreshActiveOrders()
        }
        return result
    }

    suspend fun refreshMediums(): Result<List<TakeoutMediumResponse>> {
        return when (val result = apiService.getActiveTakeoutMediums()) {
            is Result.Success -> {
                mutex.withLock { _mediumsCache.value = result.data }
                result
            }
            is Result.Error -> result
        }
    }

    suspend fun getAllMediums(): Result<List<TakeoutMediumResponse>> {
        return when (val result = apiService.getAllTakeoutMediums()) {
            is Result.Success -> {
                mutex.withLock { _mediumsCache.value = result.data }
                result
            }
            is Result.Error -> result
        }
    }

    suspend fun createMedium(request: TakeoutMediumRequest): Result<TakeoutMediumResponse> {
        val result = apiService.createTakeoutMedium(request)
        if (result is Result.Success) getAllMediums()
        return result
    }

    suspend fun updateMedium(id: Long, request: TakeoutMediumRequest): Result<TakeoutMediumResponse> {
        val result = apiService.updateTakeoutMedium(id, request)
        if (result is Result.Success) getAllMediums()
        return result
    }

    suspend fun deleteMedium(id: Long): Result<Unit> {
        val result = apiService.deleteTakeoutMedium(id)
        if (result is Result.Success) getAllMediums()
        return result
    }

    suspend fun loadLookupData(): Result<Unit> {
        val mediums = refreshMediums()
        val foods = apiService.getFoodItemsShortInfo()
        val beverages = apiService.getBeverages()
        val payments = apiService.getPaymentMethods()
        mutex.withLock {
            if (foods is Result.Success) _foodItemsCache.value = foods.data
            if (beverages is Result.Success) _beveragesCache.value = beverages.data
            if (payments is Result.Success) _paymentMethodsCache.value = payments.data
        }
        return if (mediums is Result.Error) Result.Error(mediums.message, mediums.cause) else Result.Success(Unit)
    }

    fun clearCaches() {
        _activeOrdersCache.value = emptyMap()
        _orderDetailsCache.value = emptyMap()
        _mediumsCache.value = emptyList()
        _paymentMethodsCache.value = emptyList()
        _foodItemsCache.value = emptyList()
        _beveragesCache.value = emptyList()
    }

    companion object {
        val instance: TakeoutRepository by lazy { TakeoutRepository() }
    }
}
