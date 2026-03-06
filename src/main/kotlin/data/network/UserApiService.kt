package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class UserApiService {
    private val client = HttpClientProvider.client
    private val baseUrl = AppConfig.BASE_URL

    companion object {
        private const val TAG = "UserApiService"
    }

    private fun HttpRequestBuilder.auth() {
        val token = AuthManager.getToken()
        if (!token.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        } else {
            println("[$TAG] Warning: No auth token found")
        }
    }

    // ==================== User Registration ====================

    /**
     * POST /user/registration
     *
     * Returns [UserRegistrationResponse] on 201.
     * Throws [UserRegistrationException] with the backend message on 4xx/5xx.
     */
    suspend fun registerUser(request: UserRegistrationRequest): UserRegistrationResponse {
        println("[$TAG] Registering user: ${request.email}")
        val response: HttpResponse = client.post("$baseUrl/user/registration") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> {
                println("[$TAG] Registration success: ${request.email}")
                response.body()
            }
            else -> {
                val error = try {
                    response.body<ApiErrorResponse>().message
                } catch (_: Exception) {
                    "Registration failed (HTTP ${response.status.value})"
                }
                println("[$TAG] Registration error: $error")
                throw UserRegistrationException(error)
            }
        }
    }

    // ==================== User List & Management ====================

    /**
     * GET /user-info
     * Fetch paginated list of users.
     */
    suspend fun getAllUsers(page: Int = 0, size: Int = 10, sort: String = "id"): PageResponse<UserInformationResponse> {
        println("[$TAG] Getting all users (page=$page, size=$size)")
        return try {
            client.get("$baseUrl/user-info") {
                auth()
                parameter("page", page)
                parameter("size", size)
                parameter("sort", sort)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting users: ${e.message}")
            throw e
        }
    }

    /**
     * GET /user-info/{id}
     * Fetch specific user details (including roles).
     */
    suspend fun getUserById(id: Long): UserInformationResponseWithRole {
        println("[$TAG] Getting user by id: $id")
        return try {
            client.get("$baseUrl/user-info/$id") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting user $id: ${e.message}")
            throw e
        }
    }

    /**
     * PUT /user-info/{id}
     * Updates an existing user's information and roles.
     * Returns true if successful.
     */
    suspend fun updateUser(id: Long, request: UserUpdateRequest): Boolean {
        println("[$TAG] Updating user: $id")
        val response = try {
            client.put("$baseUrl/user-info/$id") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: Exception) {
            println("[$TAG] Error updating user $id: ${e.message}")
            throw e
        }

        if (response.status.isSuccess()) {
            return true
        } else {
            val error = try {
                response.body<ApiErrorResponse>().message
            } catch (_: Exception) {
                "Update failed (HTTP ${response.status.value})"
            }
            throw UserRegistrationException(error)
        }
    }

    // ==================== Roles (for dropdown) ====================

    /**
     * GET /roles — used to populate the role dropdown.
     * Reuses the same endpoint as MenuApiService but scoped here for clarity.
     */
    suspend fun getAllRoles(): List<data.model.RoleResponse> {
        println("[$TAG] Getting all roles")
        return try {
            client.get("$baseUrl/roles") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting roles: ${e.message}")
            throw e
        }
    }
}

/** Thrown when the backend returns a non-2xx response for registration. */
class UserRegistrationException(message: String) : Exception(message)
