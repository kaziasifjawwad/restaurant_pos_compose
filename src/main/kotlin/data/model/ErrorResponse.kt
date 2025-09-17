package data.model

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val timestamp: String,
    val message: String,
    val path: String
)