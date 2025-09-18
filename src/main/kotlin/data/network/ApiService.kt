package data.network

import data.auth.AuthManager
import data.model.ErrorResponse
import data.model.LoginRequest
import data.model.LoginResponse
import data.model.MenuItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiService : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
        
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP Client: $message")
                }
            }
            level = LogLevel.ALL
        }
        
        expectSuccess = false
        
        HttpResponseValidator {
            validateResponse { response ->
                when (response.status.value) {
                    in 400..499 -> throw ClientRequestException(response, "Client error: ${response.status}")
                    in 500..599 -> throw ServerResponseException(response, "Server error: ${response.status}")
                }
            }
        }
    }

    companion object {
        private const val BASE_URL = "http://localhost:8080"
        private const val CONNECTION_TIMEOUT_MSG = "Connection timed out. Please check your internet connection."
        private const val SERVER_UNREACHABLE_MSG = "Server is unreachable. Please try again later."
        private const val UNKNOWN_ERROR_MSG = "An unexpected error occurred. Please try again."
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/user/credential/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val loginResponse = response.body<LoginResponse>()
                    if (loginResponse.jwtToken.isNotBlank()) {
                        Result.success(loginResponse)
                    } else {
                        Result.failure(Exception("Invalid token received from server"))
                    }
                }
                else -> {
                    val error = try {
                        response.body<ErrorResponse>().message
                    } catch (e: Exception) {
                        "Login failed with status: ${response.status}"
                    }
                    Result.failure(Exception(error))
                }
            }
        } catch (e: ClientRequestException) {
            Result.failure(Exception(e.message ?: "Login failed"))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception(CONNECTION_TIMEOUT_MSG))
        } catch (e: ConnectException) {
            Result.failure(Exception(SERVER_UNREACHABLE_MSG))
        } catch (e: UnknownHostException) {
            Result.failure(Exception(SERVER_UNREACHABLE_MSG))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: UNKNOWN_ERROR_MSG))
        }
    }

    suspend fun getMenu(): Result<List<MenuItem>> {
        return try {
            val token = AuthManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response: HttpResponse = client.get("$BASE_URL/menu") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }

            when (response.status) {
                HttpStatusCode.OK -> Result.success(response.body())
                HttpStatusCode.Unauthorized -> {
                    AuthManager.clearToken()
                    Result.failure(Exception("Session expired. Please login again."))
                }
                else -> {
                    val error = try {
                        response.body<ErrorResponse>().message
                    } catch (e: Exception) {
                        "Failed to load menu: ${response.status}"
                    }
                    Result.failure(Exception(error))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception(CONNECTION_TIMEOUT_MSG))
        } catch (e: ConnectException) {
            Result.failure(Exception(SERVER_UNREACHABLE_MSG))
        } catch (e: UnknownHostException) {
            Result.failure(Exception(SERVER_UNREACHABLE_MSG))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to load menu"))
        }
    }

    override fun close() {
        client.close()
        scope.cancel()
    }
}
