package data.network

import data.model.ErrorResponse
import data.model.LoginRequest
import data.model.LoginResponse
import data.model.MenuResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP Client: $message")
                }
            }
            level = LogLevel.ALL
        }
    }

    companion object {
        private const val BASE_URL = "http://localhost:8080"
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/user/credential/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                val error = response.body<ErrorResponse>()
                Result.failure(Exception(error.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMenu(token: String): Result<List<MenuResponse>> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/menu") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            
            if (response.status.value in 200..299) {
                Result.success(response.body())
            } else {
                val error = response.body<ErrorResponse>()
                Result.failure(Exception(error.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}