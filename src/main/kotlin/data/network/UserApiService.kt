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

    suspend fun registerUser(request: UserRegistrationRequest): UserRegistrationResponse {
        val response: HttpResponse = client.post("$baseUrl/user/registration") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.Created, HttpStatusCode.OK -> response.body()
            else -> throw UserRegistrationException(readError(response, "Registration failed"))
        }
    }

    suspend fun lookupEmail(email: String): EmailLookupResponse {
        return client.get("$baseUrl/user/registration/email-lookup") {
            auth()
            parameter("email", email.trim())
        }.body()
    }

    suspend fun getAllUsers(
        page: Int = 0,
        size: Int = 10,
        sort: String = "id",
        lockType: Boolean? = null,
        email: String? = null,
        mobileNumber: String? = null
    ): PageResponse<UserInformationResponse> {
        return client.get("$baseUrl/user-info") {
            auth()
            parameter("page", page)
            parameter("size", size)
            parameter("sort", sort)
            lockType?.let { parameter("lockType", it) }
            email?.trim()?.takeIf { it.isNotBlank() }?.let { parameter("email", it) }
            mobileNumber?.trim()?.takeIf { it.isNotBlank() }?.let { parameter("mobileNumber", it) }
        }.body()
    }

    suspend fun getUserById(id: Long): UserInformationResponseWithRole {
        return client.get("$baseUrl/user-info/$id") { auth() }.body()
    }

    suspend fun updateUser(id: Long, request: UserUpdateRequest): Boolean {
        val response = client.put("$baseUrl/user-info/$id") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) return true
        throw UserRegistrationException(readError(response, "Update failed"))
    }

    suspend fun lockUnlockUser(userName: String, lockType: Boolean): Boolean {
        val response = client.put("$baseUrl/user/credential/lock-unlock/$userName/$lockType") { auth() }
        if (response.status.isSuccess()) return true
        throw UserRegistrationException(readError(response, "Lock/unlock failed"))
    }

    suspend fun getAllRoles(): List<RoleResponse> {
        return client.get("$baseUrl/roles") { auth() }.body()
    }

    private suspend fun readError(response: HttpResponse, fallback: String): String {
        val raw = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching { response.body<ApiErrorResponse>().message }.getOrNull()
        return parsed?.takeIf { it.isNotBlank() }
            ?: raw.substringAfter("\"message\":\"").substringBefore("\"").takeIf { it.isNotBlank() && it != raw }
            ?: "$fallback (HTTP ${response.status.value})"
    }
}

class UserRegistrationException(message: String) : Exception(message)
