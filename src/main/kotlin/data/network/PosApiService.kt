package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

/**
 * API Service for POS (Point of Sale) operations.
 * Handles all order-related API calls.
 */
class PosApiService(
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val client = HttpClientProvider.client
    
    companion object {
        private const val TAG = "PosApiService"
    }

    suspend fun getActiveOrders(): Result<List<FoodOrderShortInfo>> {
        println("[$TAG] getActiveOrders")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/pos") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<List<FoodOrderShortInfo>>(response, "Failed to load orders")
        }
    }

    suspend fun getOrderById(id: Long): Result<FoodOrderByCustomer> {
        println("[$TAG] getOrderById: id=$id")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/pos/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<FoodOrderByCustomer>(response, "Failed to load order")
        }
    }

    suspend fun createOrder(request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        println("[$TAG] createOrder: tableId=${request.tableId}, waiterId=${request.waiterId}")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.post("$baseUrl/pos") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }
            handleResponse<FoodOrderShortInfo>(response, "Failed to create order")
        }
    }

    suspend fun updateOrder(id: Long, request: FoodOrderByCustomerRequest): Result<FoodOrderShortInfo> {
        println("[$TAG] updateOrder: id=$id, tableId=${request.tableId}, waiterId=${request.waiterId}")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.put("$baseUrl/pos/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }
            handleResponse<FoodOrderShortInfo>(response, "Failed to update order")
        }
    }

    suspend fun setPlacedOrBillPrinted(id: Long, status: OrderStatus): Result<FoodOrderByCustomer> {
        require(status == OrderStatus.ORDER_PLACED || status == OrderStatus.BILL_PRINTED) {
            "Invalid status for placed-or-bill-printed: $status"
        }
        println("[$TAG] setPlacedOrBillPrinted: id=$id, status=$status")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.put("$baseUrl/pos/placed-or-bill-printed/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                url { parameters.append("orderStatus", status.name) }
            }
            handleResponse<FoodOrderByCustomer>(response, "Failed to update order status")
        }
    }

    suspend fun setPaidOrCancel(
        id: Long,
        status: OrderStatus,
        paymentMethod: PaymentMethod? = null
    ): Result<FoodOrderByCustomer> {
        require(status == OrderStatus.PAID || status == OrderStatus.CANCELED) {
            "Invalid status for paid-or-cancel: $status"
        }
        println("[$TAG] setPaidOrCancel: id=$id, status=$status, paymentMethod=$paymentMethod")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.put("$baseUrl/pos/paid-or-cancel/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                url {
                    parameters.append("orderStatus", status.name)
                    if (status == OrderStatus.PAID && paymentMethod != null) {
                        parameters.append("paymentMethod", paymentMethod.name)
                    }
                }
            }
            handleResponse<FoodOrderByCustomer>(response, "Failed to update order status")
        }
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethodResponse>> {
        println("[$TAG] getPaymentMethods")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/pos/payment-methods") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<List<PaymentMethodResponse>>(response, "Failed to load payment methods")
        }
    }

    suspend fun getAllPaymentMethods(): Result<List<PaymentMethodResponse>> {
        println("[$TAG] getAllPaymentMethods")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/pos/payment-methods/all") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<List<PaymentMethodResponse>>(response, "Failed to load payment methods")
        }
    }

    suspend fun createPaymentMethod(request: PaymentMethodRequest): Result<PaymentMethodResponse> {
        println("[$TAG] createPaymentMethod: methodCode=${request.methodCode}")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.post("$baseUrl/pos/payment-methods") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }
            handleResponse<PaymentMethodResponse>(response, "Failed to create payment method")
        }
    }

    suspend fun updatePaymentMethod(id: Long, request: PaymentMethodRequest): Result<PaymentMethodResponse> {
        println("[$TAG] updatePaymentMethod: id=$id, methodCode=${request.methodCode}")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.put("$baseUrl/pos/payment-methods/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }
            handleResponse<PaymentMethodResponse>(response, "Failed to update payment method")
        }
    }

    suspend fun setDefaultPaymentMethod(id: Long): Result<PaymentMethodResponse> {
        println("[$TAG] setDefaultPaymentMethod: id=$id")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.put("$baseUrl/pos/payment-methods/$id/default") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<PaymentMethodResponse>(response, "Failed to set default payment method")
        }
    }

    suspend fun deletePaymentMethod(id: Long): Result<Unit> {
        println("[$TAG] deletePaymentMethod: id=$id")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.delete("$baseUrl/pos/payment-methods/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> Unit
                else -> throw Exception("Failed to delete payment method: ${tryParseError(response)}")
            }
        }
    }

    suspend fun getWaiters(): Result<List<WaiterInfo>> {
        println("[$TAG] getWaiters")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/user-info/waiter") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
            }
            handleResponse<List<WaiterInfo>>(response, "Failed to load waiters")
        }
    }

    suspend fun getTables(): Result<List<TableInfo>> {
        println("[$TAG] getTables")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/table") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                url { parameters.append("unpaged", "true") }
            }
            val pageResponse = handleResponse<PageTableResponse>(response, "Failed to load tables")
            pageResponse.content
        }
    }

    suspend fun getFoodItemsShortInfo(): Result<List<FoodItemShortInfo>> {
        println("[$TAG] getFoodItemsShortInfo")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/food/item") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                url { parameters.append("unpaged", "true") }
            }
            val pageResponse = handleResponse<PageFoodItemResponse>(response, "Failed to load food items")
            pageResponse.content.map { item ->
                val prices = item.foodPrices.map { price ->
                    FoodPriceInfo(
                        foodSize = price.foodSize,
                        foodPrice = price.foodPrice,
                        isDefault = price.isDefault
                    )
                }
                FoodItemShortInfo(
                    id = item.id,
                    name = item.name,
                    itemNumber = item.itemNumber.toShort(),
                    foodPrices = prices,
                    defaultPrice = item.defaultPrice?.let { price ->
                        FoodPriceInfo(
                            foodSize = price.foodSize,
                            foodPrice = price.foodPrice,
                            isDefault = true
                        )
                    } ?: prices.firstOrNull { it.isDefault }
                )
            }
        }
    }

    suspend fun getBeverages(): Result<List<BeverageResponse>> {
        println("[$TAG] getBeverages")
        return executeRequest {
            val token = requireToken()
            val response: HttpResponse = client.get("$baseUrl/food/beverage") {
                header(HttpHeaders.Authorization, "Bearer $token")
                accept(ContentType.Application.Json)
                url { parameters.append("unpaged", "true") }
            }
            val pageResponse = handleResponse<PageBeverageResponse>(response, "Failed to load beverages")
            pageResponse.content
        }
    }

    private fun requireToken(): String {
        return AuthManager.getToken() ?: throw Exception("Not authenticated")
    }

    private suspend inline fun <T> executeRequest(block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: Exception) {
            println("[$TAG] Error: ${e.message}")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse, errorPrefix: String): T {
        println("[$TAG] Response status: ${response.status}")
        println("[$TAG] Response Content-Type: ${response.contentType()}")
        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                try {
                    response.body()
                } catch (e: Exception) {
                    println("[$TAG] Normal deserialization failed: ${e.message}")
                    try {
                        val bodyText = response.bodyAsText()
                        println("[$TAG] Response body: $bodyText")
                        val json = Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = true
                            explicitNulls = false
                        }
                        json.decodeFromString<T>(bodyText)
                    } catch (parseError: Exception) {
                        println("[$TAG] Manual JSON parsing failed: ${parseError.message}")
                        throw Exception("$errorPrefix: Failed to parse response - ${parseError.message}")
                    }
                }
            }
            HttpStatusCode.Unauthorized -> {
                println("[$TAG] Unauthorized - clearing token")
                AuthManager.clearToken()
                throw Exception("Session expired. Please login again.")
            }
            HttpStatusCode.BadRequest -> throw Exception("$errorPrefix: ${tryParseError(response)}")
            HttpStatusCode.NotFound -> throw Exception("$errorPrefix: Not found")
            HttpStatusCode.Conflict -> throw Exception("$errorPrefix: ${tryParseError(response)}")
            else -> throw Exception("$errorPrefix: ${response.status} - ${tryParseError(response)}")
        }
    }

    private suspend fun tryParseError(response: HttpResponse): String {
        return try {
            response.body<ErrorResponse>().message
        } catch (_: Exception) {
            try {
                response.bodyAsText()
            } catch (_: Exception) {
                "Unknown error"
            }
        }
    }
}
