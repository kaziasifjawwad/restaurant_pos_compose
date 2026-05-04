package data.network

import data.auth.AuthManager
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class UnauthorizedSessionException(
    message: String = "Your session has expired. Please login again."
) : RuntimeException(message)

object HttpClientProvider {
    val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                isLenient = true
                encodeDefaults = false
                explicitNulls = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    AuthManager.clearToken()
                    throw UnauthorizedSessionException()
                }
            }
            handleResponseExceptionWithRequest { exception, _ ->
                val responseException = exception as? ResponseException
                if (responseException?.response?.status == HttpStatusCode.Unauthorized) {
                    AuthManager.clearToken()
                    throw UnauthorizedSessionException()
                }
            }
        }
        expectSuccess = false
    }

    // Call this only once, when the whole app is exiting.
    fun shutdown() {
        client.close()
    }
}
