package data.model

import kotlinx.serialization.Serializable

// ==================== Generic API Responses ====================

/**
 * Generic success wrapper returned by PUT/POST endpoints that respond with {"success": true}.
 *
 * kotlinx.serialization cannot deserialize Map<String, Any> because `Any` is polymorphic
 * and requires a type discriminator that our backend never sends. Use this instead everywhere
 * the server returns a simple {"success": …} body.
 */
@Serializable
data class SuccessResponse(val success: Boolean = false)

// ==================== Menu Models ====================

@Serializable
data class MenuResponse(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val menuOrder: Int = 0,
    val path: String? = null,
    val iconSrc: String? = null,
    val showInMenuList: Boolean = true,
    val status: Boolean = true,
    val menuCode: String? = null,
    val localizationCode: String? = null,
    val restrictedToRoles: List<RoleResponse> = emptyList(),
    val children: List<MenuResponse> = emptyList()
)

@Serializable
data class MenuRequest(
    val id: Long? = null,
    val name: String,
    val parentId: Long? = null,
    val menuOrder: Int = 0,
    val path: String? = null,
    val iconSrc: String? = null,
    val showInMenuList: Boolean = true,
    val status: Boolean = true,
    val menuCode: String? = null,
    val localizationCode: String? = null,
    val restrictedToRoles: List<RoleRequest> = emptyList()
)

// ==================== Role Models ====================

@Serializable
data class RoleResponse(
    val id: Long = 0,
    val name: String,
    val description: String? = null
)

@Serializable
data class RoleRequest(
    val id: Long? = null,
    val roleName: String
)

// ==================== Menu-Role Models ====================

@Serializable
data class MenuRoleResponse(
    val id: Long = 0,
    val menuId: Long,
    val roleId: Long,
    val active: Boolean
)

@Serializable
data class MenuRoleRequest(
    val menuId: Long,
    val roleId: Long,
    val active: Boolean,
    val name: String? = null
)

// ==================== Permission Models ====================

@Serializable
data class PermissionResponse(
    val id: Long = 0,
    val name: String,
    val groupName: String,
    val localizationCode: String? = null,
    val menuId: Long? = null,
    val permissionOrder: Int = 0,
    val status: Boolean = true,
    var active: Boolean = false, // For UI state
    var displayName: String? = null // For UI display
)

@Serializable
data class PermissionRequest(
    val groupName: String,
    val name: String,
    val localizationCode: String? = null,
    val menuId: Long? = null,
    val permissionOrder: Int = 0,
    val status: Boolean = true
)

@Serializable
data class PermissionRoleRequest(
    val permissionId: Long,
    val roleId: Long,
    val active: Boolean
)

@Serializable
data class PermissionRoleResponse(
    val id: Long = 0,
    val permissionId: Long,
    val roleId: Long,
    val active: Boolean
)

// ==================== UI Models ====================

/**
 * Tree node for menu display
 */
data class MenuTreeNode(
    val key: String,
    val title: String,
    val origin: MenuResponse,
    val children: List<MenuTreeNode> = emptyList(),
    var isExpanded: Boolean = true,
    var isChecked: Boolean = false,
    val restrictedToRoles: List<Long> = emptyList()
)

/**
 * Permission group for display
 */
data class PermissionGroup(
    val id: Long,
    val permissionGroup: String,
    val title: String,
    val permission: List<PermissionResponse>,
    var edit: Boolean = false
)
