package data.network

import data.auth.AuthManager
import data.model.*
import io.ktor.client.call.body
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * API Service for Food Item operations
 * Handles CRUD operations and lookup data fetching
 */
class FoodItemApiService(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val client = HttpClientProvider.client
    
    companion object {
        private const val TAG = "FoodItemApiService"
    }

    // ==================== Food Items ====================

    /**
     * Get paginated list of food items
     */
    suspend fun getFoodItems(page: Int = 0, size: Int = 20): PageFoodItemResponse {
        println("[$TAG] getFoodItems: page=$page, size=$size")
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

        println("[$TAG] getFoodItems response status: ${response.status}")
        return handleResponse(response, "Failed to load food items")
    }

    /**
     * Get a single food item by ID
     */
    suspend fun getFoodItemById(id: Long): FoodItemResponse {
        println("[$TAG] getFoodItemById: id=$id")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/item/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }

        println("[$TAG] getFoodItemById response status: ${response.status}")
        return handleResponse(response, "Failed to load food item")
    }

    /**
     * Create a new food item
     */
    suspend fun createFoodItem(request: FoodItemRequest): FoodItemResponse {
        println("[$TAG] createFoodItem: name=${request.name}, itemNumber=${request.itemNumber}")
        println("[$TAG] createFoodItem: categories=${request.foodCategorySet.size}, variants=${request.foodPriceSet.size}")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.post("$baseUrl/food/item") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(request)
        }

        println("[$TAG] createFoodItem response status: ${response.status}")
        return handleResponse(response, "Failed to create food item")
    }

    /**
     * Update an existing food item
     */
    suspend fun updateFoodItem(id: Long, request: FoodItemRequest): FoodItemResponse {
        println("[$TAG] updateFoodItem: id=$id, name=${request.name}")
        println("[$TAG] updateFoodItem: categories=${request.foodCategorySet.size}, variants=${request.foodPriceSet.size}")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.put("$baseUrl/food/item/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(request)
        }

        println("[$TAG] updateFoodItem response status: ${response.status}")
        return handleResponse(response, "Failed to update food item")
    }

    // ==================== Lookup APIs ====================

    /**
     * Get all ingredients for lookup dropdown
     */
    suspend fun getIngredients(): List<Ingredient> {
        println("[$TAG] getIngredients: fetching all ingredients")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/ingredient") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", "0")
                parameters.append("size", "1000")
                parameters.append("unpaged", "true")
            }
        }

        println("[$TAG] getIngredients response status: ${response.status}")
        val pageResponse: PageIngredientResponse = handleResponse(response, "Failed to load ingredients")
        println("[$TAG] getIngredients: loaded ${pageResponse.content.size} ingredients")
        return pageResponse.content
    }

    /**
     * Get all food categories for lookup dropdown
     */
    suspend fun getCategories(): List<FoodCategory> {
        println("[$TAG] getCategories: fetching all categories")
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")

        val response: HttpResponse = client.get("$baseUrl/food/category") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", "0")
                parameters.append("size", "100")
                parameters.append("unpaged", "true")
            }
        }

        println("[$TAG] getCategories response status: ${response.status}")
        val pageResponse: PageCategoryResponse = handleResponse(response, "Failed to load categories")
        println("[$TAG] getCategories: loaded ${pageResponse.content.size} categories")
        return pageResponse.content
    }

    // ==================== Ingredient CRUD ====================

    /**
     * GET /food/ingredient (paginated)
     */
    suspend fun getIngredientsPaged(page: Int = 0, size: Int = 20): PageIngredientResponse {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.get("$baseUrl/food/ingredient") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                parameters.append("sort", "id")
            }
        }
        return handleResponse(response, "Failed to load ingredients")
    }

    /**
     * GET /food/ingredient/{id}
     */
    suspend fun getIngredientById(id: Long): Ingredient {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.get("$baseUrl/food/ingredient/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
        return handleResponse(response, "Failed to load ingredient")
    }

    /**
     * POST /food/ingredient
     */
    suspend fun createIngredient(name: String, description: String): Ingredient {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.post("$baseUrl/food/ingredient") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(IngredientRequest(name, description))
        }
        return handleResponse(response, "Failed to create ingredient")
    }

    /**
     * PUT /food/ingredient/{id}
     */
    suspend fun updateIngredient(id: Long, name: String, description: String): Ingredient {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.put("$baseUrl/food/ingredient/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(IngredientRequest(name, description))
        }
        return handleResponse(response, "Failed to update ingredient")
    }

    /**
     * DELETE /food/ingredient/{id}
     */
    suspend fun deleteIngredient(id: Long) {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.delete("$baseUrl/food/ingredient/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            val error = tryParseError(response)
            throw Exception("Failed to delete ingredient: $error")
        }
    }

    // ==================== Food Category CRUD ====================

    /**
     * GET /food/category (paginated)
     */
    suspend fun getCategoriesPaged(page: Int = 0, size: Int = 20): PageCategoryResponse {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.get("$baseUrl/food/category") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                parameters.append("sort", "id")
            }
        }
        return handleResponse(response, "Failed to load categories")
    }

    /**
     * GET /food/category/{id}
     */
    suspend fun getCategoryById(id: Long): FoodCategory {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.get("$baseUrl/food/category/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            accept(ContentType.Application.Json)
        }
        return handleResponse(response, "Failed to load category")
    }

    /**
     * POST /food/category
     */
    suspend fun createCategory(name: String, description: String): FoodCategory {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.post("$baseUrl/food/category") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(CategoryRequest(name, description))
        }
        return handleResponse(response, "Failed to create category")
    }

    /**
     * PUT /food/category/{id}
     */
    suspend fun updateCategory(id: Long, name: String, description: String): FoodCategory {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.put("$baseUrl/food/category/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(CategoryRequest(name, description))
        }
        return handleResponse(response, "Failed to update category")
    }

    /**
     * DELETE /food/category/{id}
     */
    suspend fun deleteCategory(id: Long) {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response: HttpResponse = client.delete("$baseUrl/food/category/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            val error = tryParseError(response)
            throw Exception("Failed to delete category: $error")
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
