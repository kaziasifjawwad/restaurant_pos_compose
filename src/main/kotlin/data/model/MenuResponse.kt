package data.model

import kotlinx.serialization.Serializable

@Serializable
data class MenuResponse(
    val id: String,
    val name: String,
    val menuOrder: Int,
    val path: String,
    val showInMenuList: Boolean? = true,
    val menuCode: String,
    val restrictedToRoles: List<String>,
    val children: List<MenuResponse> = emptyList(),
    val parentId: String? = null,
    val status: Boolean? = true
)