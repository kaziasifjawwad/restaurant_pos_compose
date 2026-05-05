package data.network

import data.auth.AuthManager
import data.model.*
import io.ktor.client.call.body
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API Service for Beverage operations
 * Handles CRUD operations for beverages
 */
class BeverageApiService(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val client = HttpClientProvider.client
    
    companion object {
        private const val TAG = "BeverageApiService"
    }

    // ==================== Beverages ====================

    /**
     * Get paginated list of beverages
     */
    suspend fun getBeverages(page: Int = 0, size: Int = 20): PageBeverageResponse {
        println("[$TAG] getBeverages: page=$page, size=$size")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/beverage") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                parameters.append("sort", "id")
            }
        }

        println("[$TAG] getBeverages response status: ${response.status}")
        return handleResponse(response, "Failed to load beverages")
    }

    /**
     * Get a single beverage by ID
     */
    suspend fun getBeverageById(id: Long): BeverageResponse {
        println("[$TAG] getBeverageById: id=$id")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/beverage/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }

        println("[$TAG] getBeverageById response status: ${response.status}")
        return handleResponse(response, "Failed to load beverage")
    }

    /**
     * Create a new beverage
     */
    suspend fun createBeverage(request: BeverageRequest): BeverageResponse {
        println("[$TAG] createBeverage: name=${request.name}, prices=${request.prices.size}")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.post("$baseUrl/food/beverage") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(request)
        }

        println("[$TAG] createBeverage response status: ${response.status}")
        return handleResponse(response, "Failed to create beverage")
    }

    /**
     * Update an existing beverage
     */
    suspend fun updateBeverage(id: Long, request: BeverageRequest): BeverageResponse {
        println("[$TAG] updateBeverage: id=$id, name=${request.name}")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.put("$baseUrl/food/beverage/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(request)
        }

        println("[$TAG] updateBeverage response status: ${response.status}")
        return handleResponse(response, "Failed to update beverage")
    }

    /**
     * Delete a beverage by ID
     */
    suspend fun deleteBeverage(id: Long) {
        println("[$TAG] deleteBeverage: id=$id")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.delete("$baseUrl/food/beverage/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }

        println("[$TAG] deleteBeverage response status: ${response.status}")
        
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.NoContent -> {
                println("[$TAG] deleteBeverage: Success")
            }
            HttpStatusCode.Unauthorized -> {
                println("[$TAG] Unauthorized - clearing token")
                AuthManager.clearToken()
                throw Exception("Session expired. Please login again.")
            }
            else -> {
                val error = tryParseError(response)
                println("[$TAG] deleteBeverage error: $error")
                throw Exception("Failed to delete beverage: $error")
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Generic response handler with proper error handling
     */
    private suspend inline fun <reified T> handleResponse(
        response: HttpResponse,
        errorPrefix: String
    ): T {
        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                response.body()
            }
            HttpStatusCode.Unauthorized -> {
                println("[$TAG] Unauthorized - clearing token")
                AuthManager.clearToken()
                throw Exception("Session expired. Please login again.")
            }
            HttpStatusCode.BadRequest -> {
                val error = tryParseError(response)
                println("[$TAG] Bad Request: $error")
                throw Exception("$errorPrefix: $error")
            }
            HttpStatusCode.NotFound -> {
                val error = tryParseError(response)
                println("[$TAG] Not Found: $error")
                throw Exception("$errorPrefix: Resource not found")
            }
            HttpStatusCode.Conflict -> {
                val error = tryParseError(response)
                println("[$TAG] Conflict: $error")
                throw Exception("$errorPrefix: $error")
            }
            else -> {
                val error = tryParseError(response)
                println("[$TAG] Error ${response.status}: $error")
                throw Exception("$errorPrefix: ${response.status}")
            }
        }
    }

    /**
     * Try to parse error message from response
     */
    private suspend fun tryParseError(response: HttpResponse): String {
        return try {
            val errorResponse = response.body<ErrorResponse>()
            errorResponse.message ?: errorResponse.error ?: "Unknown error"
        } catch (_: Exception) {
            "Unknown error"
        }
    }
}


