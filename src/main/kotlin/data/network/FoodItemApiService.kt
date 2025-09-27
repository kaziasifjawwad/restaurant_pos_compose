package data.network

import data.auth.AuthManager
import data.model.ErrorResponse
import data.model.PageFoodItemResponse
import io.ktor.client.call.body
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*

class FoodItemApiService(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val client = HttpClientProvider.client

    suspend fun getFoodItems(page: Int = 0, size: Int = 20): PageFoodItemResponse {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/item") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                parameters.append("sort", "id")
            }
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> {
                AuthManager.clearToken()
                throw Exception("Session expired. Please login again.")
            }
            else -> {
                val error = try {
                    response.body<ErrorResponse>().message
                } catch (_: Exception) {
                    "Failed to load food items: ${response.status}"
                }
                throw Exception(error)
            }
        }
    }
}
