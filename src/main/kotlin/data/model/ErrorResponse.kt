package data.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val timestamp: String? = null,
    val message: String? = null,
    val error: String? = null,
    val path: String? = null,
    val status: Int? = null
)
