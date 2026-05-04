package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.DashboardFullResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class DashboardApiService {
    private val client = HttpClientProvider.client
    private val baseUrl = AppConfig.BASE_URL

    private fun HttpRequestBuilder.auth() {
        val token = AuthManager.getToken()
        if (!token.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $token")
    }

    suspend fun getFull(dateFrom: String? = null, dateTo: String? = null): DashboardFullResponse {
        return client.get("$baseUrl/dashboard/full") {
            auth()
            if (!dateFrom.isNullOrBlank()) parameter("dateFrom", dateFrom)
            if (!dateTo.isNullOrBlank()) parameter("dateTo", dateTo)
        }.body()
    }
}
