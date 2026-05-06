package data.model

import kotlinx.serialization.Serializable

@Serializable
data class TableResponse(
    val tableNumber: Int = 0,
    val description: String = "",
    val capacity: Int = 0,
    val id: Long = 0,
    val isAvailable: Boolean = true
)

@Serializable
data class TableRequest(
    val tableNumber: Int,
    val description: String = "",
    val capacity: Int
)

@Serializable
data class TableUpdateRequest(
    val description: String = "",
    val isAvailable: Boolean = true,
    val capacity: Int
)
