package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.PosOrderDetailResponse
import data.model.PosReportPage
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

class ReportApiService {
    private val client = HttpClientProvider.client
    private val baseUrl = AppConfig.BASE_URL
    
    companion object {
        private const val TAG = "ReportApiService"
    }

    private fun HttpRequestBuilder.auth() {
        val token = AuthManager.getToken()
        if (!token.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        } else {
            println("[$TAG] Warning: No auth token found")
        }
    }
    
    /**
     * Get paginated POS reports
     */
    suspend fun getPosReports(
        page: Int = 0,
        size: Int = 10,
        unpaged: Boolean = false
    ): PosReportPage {
        println("[$TAG] Getting POS reports: page=$page, size=$size")
        return try {
            client.get("$baseUrl/report/pos") {
                auth()
                parameter("page", page)
                parameter("size", size)
                parameter("unpaged", unpaged)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting POS reports: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get single POS order detail
     */
    suspend fun getPosOrderDetail(orderId: String): PosOrderDetailResponse {
        println("[$TAG] Getting POS order detail: $orderId")
        return try {
            client.get("$baseUrl/pos/$orderId") {
                auth()
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting order detail: ${e.message}")
            throw e
        }
    }
    
    /**
     * Download POS report as PDF
     */
    suspend fun downloadPosReportPdf(
        dateFrom: String?,
        dateTo: String?,
        outputFile: File
    ): Boolean {
        println("[$TAG] Downloading PDF report: from=$dateFrom, to=$dateTo")
        return try {
            val response: HttpResponse = client.get("$baseUrl/report/pos/pdf") {
                auth()
                if (dateFrom != null) parameter("dateFrom", dateFrom)
                if (dateTo != null) parameter("dateTo", dateTo)
            }
            
            if (response.status.isSuccess()) {
                val bytes = response.body<ByteArray>()
                outputFile.writeBytes(bytes)
                println("[$TAG] PDF saved to: ${outputFile.absolutePath}")
                true
            } else {
                println("[$TAG] Failed to download PDF: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("[$TAG] Error downloading PDF: ${e.message}")
            throw e
        }
    }
}
