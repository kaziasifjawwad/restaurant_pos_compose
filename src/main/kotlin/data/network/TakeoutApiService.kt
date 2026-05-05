package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class TakeoutApiService(private val baseUrl: String = AppConfig.BASE_URL) {
    private val client = HttpClientProvider.client
    private val posApiService = PosApiService(baseUrl)

    suspend fun getActiveTakeoutOrders(): Result<List<TakeoutOrderShortInfo>> = executeRequest {
        val response = client.get("$baseUrl/takeout/active") { authJson() }
        handleResponse(response, "Failed to load takeout orders")
    }

    suspend fun getTakeoutOrders(page: Int = 0, size: Int = 20): Result<PageTakeoutOrderShortInfoResponse> = executeRequest {
        val response = client.get("$baseUrl/takeout") {
            authJson()
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
            }
        }
        handleResponse(response, "Failed to search takeout orders")
    }

    suspend fun getTakeoutReport(page: Int = 0, size: Int = 20, dateFrom: String? = null, dateTo: String? = null, mediumCode: String? = null): Result<TakeoutReportDashboardResponse> = executeRequest {
        val response = client.get("$baseUrl/report/takeout") {
            authJson()
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                if (!dateFrom.isNullOrBlank()) parameters.append("dateFrom", dateFrom)
                if (!dateTo.isNullOrBlank()) parameters.append("dateTo", dateTo)
                if (!mediumCode.isNullOrBlank()) parameters.append("mediumCode", mediumCode)
            }
        }
        handleResponse(response, "Failed to load takeout report")
    }

    suspend fun getTakeoutOrderById(id: Long): Result<TakeoutOrderResponse> = executeRequest {
        val response = client.get("$baseUrl/takeout/$id") { authJson() }
        handleResponse(response, "Failed to load takeout order")
    }

    suspend fun createTakeoutOrder(request: TakeoutOrderRequest): Result<TakeoutOrderShortInfo> = executeRequest {
        val response = client.post("$baseUrl/takeout") { authJson(); setBody(request) }
        handleResponse(response, "Failed to create takeout order")
    }

    suspend fun updateTakeoutOrder(id: Long, request: TakeoutOrderRequest): Result<TakeoutOrderShortInfo> = executeRequest {
        val response = client.put("$baseUrl/takeout/$id") { authJson(); setBody(request) }
        handleResponse(response, "Failed to update takeout order")
    }

    suspend fun updateTakeoutStatus(id: Long, request: TakeoutStatusUpdateRequest): Result<TakeoutOrderResponse> = executeRequest {
        val response = client.put("$baseUrl/takeout/$id/status") { authJson(); setBody(request) }
        handleResponse(response, "Failed to update takeout status")
    }

    suspend fun updateTakeoutPayment(id: Long, request: TakeoutPaymentUpdateRequest): Result<TakeoutOrderResponse> = executeRequest {
        val response = client.put("$baseUrl/takeout/$id/payment") { authJson(); setBody(request) }
        handleResponse(response, "Failed to update takeout payment")
    }

    suspend fun cancelTakeoutOrder(id: Long, reason: String): Result<TakeoutOrderResponse> = executeRequest {
        val response = client.put("$baseUrl/takeout/$id/cancel") { authJson(); setBody(TakeoutCancelRequest(reason)) }
        handleResponse(response, "Failed to cancel takeout order")
    }

    suspend fun getActiveTakeoutMediums(): Result<List<TakeoutMediumResponse>> = executeRequest {
        val response = client.get("$baseUrl/takeout/medium") { authJson() }
        handleResponse(response, "Failed to load takeout mediums")
    }

    suspend fun getAllTakeoutMediums(): Result<List<TakeoutMediumResponse>> = executeRequest {
        val response = client.get("$baseUrl/takeout/medium/all") { authJson() }
        handleResponse(response, "Failed to load takeout mediums")
    }

    suspend fun createTakeoutMedium(request: TakeoutMediumRequest): Result<TakeoutMediumResponse> = executeRequest {
        val response = client.post("$baseUrl/takeout/medium") { authJson(); setBody(request) }
        handleResponse(response, "Failed to create takeout medium")
    }

    suspend fun updateTakeoutMedium(id: Long, request: TakeoutMediumRequest): Result<TakeoutMediumResponse> = executeRequest {
        val response = client.put("$baseUrl/takeout/medium/$id") { authJson(); setBody(request) }
        handleResponse(response, "Failed to update takeout medium")
    }

    suspend fun deleteTakeoutMedium(id: Long): Result<Unit> = executeRequest {
        val response = client.delete("$baseUrl/takeout/medium/$id") { authJson() }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            throw Exception("Failed to delete takeout medium: ${tryParseError(response)}")
        }
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethodResponse>> = posApiService.getPaymentMethods()
    suspend fun getFoodItemsShortInfo(): Result<List<FoodItemShortInfo>> = posApiService.getFoodItemsShortInfo()
    suspend fun getBeverages(): Result<List<BeverageResponse>> = posApiService.getBeverages()

    private fun HttpRequestBuilder.authJson() {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }

    private suspend inline fun <T> executeRequest(block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error", e)
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse, errorPrefix: String): T = when (response.status) {
        HttpStatusCode.OK, HttpStatusCode.Created -> response.body()
        HttpStatusCode.Unauthorized -> {
            AuthManager.clearToken()
            throw Exception("Session expired. Please login again.")
        }
        else -> throw Exception("$errorPrefix: ${tryParseError(response)}")
    }

    private suspend fun tryParseError(response: HttpResponse): String {
        val bodyText = runCatching { response.bodyAsText() }.getOrDefault("")
        return runCatching {
            val parsed = response.body<ErrorResponse>()
            parsed.message ?: parsed.error ?: extractJsonMessage(bodyText) ?: bodyText.ifBlank { response.status.description }
        }.getOrDefault(extractJsonMessage(bodyText) ?: bodyText.ifBlank { response.status.description })
    }

    private fun extractJsonMessage(bodyText: String): String? {
        if (bodyText.isBlank()) return null
        val messageRegex = Regex(""""message"\s*:\s*"([^"]+)"""")
        val errorRegex = Regex(""""error"\s*:\s*"([^"]+)"""")
        return messageRegex.find(bodyText)?.groupValues?.getOrNull(1)
            ?: errorRegex.find(bodyText)?.groupValues?.getOrNull(1)
    }
}
