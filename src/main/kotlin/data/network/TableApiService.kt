package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.ApiErrorResponse
import data.model.PageResponse
import data.model.TableRequest
import data.model.TableResponse
import data.model.TableUpdateRequest
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class TableApiService {
    private val client = HttpClientProvider.client
    private val baseUrl = AppConfig.BASE_URL

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        AuthManager.getToken()?.takeIf { it.isNotBlank() }?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun getTables(page: Int = 0, size: Int = 10): PageResponse<TableResponse> {
        return client.get("$baseUrl/table") {
            auth()
            parameter("page", page)
            parameter("size", size)
            parameter("sort", "tableNumber")
            parameter("unpaged", false)
        }.body()
    }

    suspend fun getTableById(id: String): TableResponse {
        return client.get("$baseUrl/table/$id") { auth() }.body()
    }

    suspend fun createTable(request: TableRequest): TableResponse {
        val response: HttpResponse = client.post("$baseUrl/table") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) return response.body()
        throw TableApiException(readError(response, "Table creation failed"))
    }

    suspend fun updateTable(id: String, request: TableUpdateRequest): TableResponse {
        val response: HttpResponse = client.put("$baseUrl/table/$id") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status.isSuccess()) return response.body()
        throw TableApiException(readError(response, "Table update failed"))
    }

    suspend fun deleteTable(id: String): Boolean {
        val response: HttpResponse = client.delete("$baseUrl/table/$id") { auth() }
        if (response.status.isSuccess()) return true
        throw TableApiException(readError(response, "Table delete failed"))
    }

    private suspend fun readError(response: HttpResponse, fallback: String): String {
        val raw = runCatching { response.bodyAsText() }.getOrDefault("")
        val parsed = runCatching { response.body<ApiErrorResponse>().message }.getOrNull()
        return parsed?.takeIf { it.isNotBlank() }
            ?: raw.substringAfter("\"message\":\"").substringBefore("\"").takeIf { it.isNotBlank() && it != raw }
            ?: "$fallback (HTTP ${response.status.value})"
    }
}

class TableApiException(message: String) : Exception(message)
