package data.model

import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(
    val menuCode: String,
    val name: String,
    val path: String? = null,
    val children: List<MenuItem> = emptyList()
)
