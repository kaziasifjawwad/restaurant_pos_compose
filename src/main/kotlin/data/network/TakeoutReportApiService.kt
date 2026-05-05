package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.ErrorResponse
import data.model.Result
import data.model.TakeoutOrderStatus
import data.model.TakeoutReportDashboardResponse
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class TakeoutReportApiService(private val baseUrl: String = AppConfig.BASE_URL) {
    private val client = HttpClientProvider.client

    suspend fun getReport(
        page: Int = 0,
        size: Int = 20,
        dateFrom: String? = null,
        dateTo: String? = null,
        mediumCode: String? = null,
        orderStatus: TakeoutOrderStatus? = null
    ): Result<TakeoutReportDashboardResponse> = try {
        val token = AuthManager.getToken() ?: throw Exception("Not authenticated")
        val response = client.get("$baseUrl/report/takeout") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                if (!dateFrom.isNullOrBlank()) parameters.append("dateFrom", dateFrom)
                if (!dateTo.isNullOrBlank()) parameters.append("dateTo", dateTo)
                if (!mediumCode.isNullOrBlank()) parameters.append("mediumCode", mediumCode)
                if (orderStatus != null) parameters.append("orderStatus", orderStatus.name)
            }
        }
        if (response.status == HttpStatusCode.OK) Result.Success(response.body())
        else Result.Error("Failed to load takeout report: ${parseError(response)}")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to load takeout report", e)
    }

    private suspend fun parseError(response: HttpResponse): String {
        val text = runCatching { response.bodyAsText() }.getOrDefault("")
        return runCatching {
            val error = response.body<ErrorResponse>()
            error.message ?: error.error ?: text.ifBlank { response.status.description }
        }.getOrDefault(text.ifBlank { response.status.description })
    }
}
