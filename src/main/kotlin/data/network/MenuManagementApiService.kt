package data.network

import data.auth.AuthManager
import data.config.AppConfig
import data.model.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class MenuApiService {
    private val client = HttpClientProvider.client
    private val baseUrl = AppConfig.BASE_URL
    
    companion object {
        private const val TAG = "MenuApiService"
    }

    private fun HttpRequestBuilder.auth() {
        val token = AuthManager.getToken()
        if (!token.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $token")
        } else {
            println("[$TAG] Warning: No auth token found")
        }
    }
    
    // ==================== Menu APIs ====================
    
    /**
     * Get all menus with sub-menus
     */
    suspend fun getAllMenus(): List<MenuResponse> {
        println("[$TAG] Getting all menus")
        return try {
            client.get("$baseUrl/menu") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting menus: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get menu by ID
     */
    suspend fun getMenuById(menuId: Long): MenuResponse {
        println("[$TAG] Getting menu by id: $menuId")
        return try {
            client.get("$baseUrl/menu/$menuId") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting menu: ${e.message}")
            throw e
        }
    }
    
    /**
     * Create new menu
     */
    suspend fun createMenu(menu: MenuRequest): MenuResponse {
        println("[$TAG] Creating menu: ${menu.name}")
        return try {
            client.post("$baseUrl/menu") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(menu)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error creating menu: ${e.message}")
            throw e
        }
    }
    
    /**
     * Update existing menu
     */
    suspend fun updateMenu(menu: MenuRequest): MenuResponse {
        println("[$TAG] Updating menu: ${menu.name}")
        return try {
            client.put("$baseUrl/menu") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(menu)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error updating menu: ${e.message}")
            throw e
        }
    }
    
    /**
     * Delete menu by ID
     */
    suspend fun deleteMenu(menuId: Long) {
        println("[$TAG] Deleting menu: $menuId")
        try {
            client.delete("$baseUrl/menu/$menuId") { auth() }
        } catch (e: Exception) {
            println("[$TAG] Error deleting menu: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get menus by role name
     */
    suspend fun getMenusByRole(roleName: String): List<MenuResponse> {
        println("[$TAG] Getting menus for role: $roleName")
        return try {
            client.get("$baseUrl/menu/role/$roleName") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting menus by role: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get menus for current user
     */
    suspend fun getMenusForUser(): List<MenuResponse> {
        println("[$TAG] Getting menus for current user")
        return try {
            client.get("$baseUrl/menu/user") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting user menus: ${e.message}")
            throw e
        }
    }
    
    // ==================== Role APIs ====================
    
    /**
     * Get all roles
     */
    suspend fun getAllRoles(): List<RoleResponse> {
        println("[$TAG] Getting all roles")
        return try {
            client.get("$baseUrl/roles") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting roles: ${e.message}")
            throw e
        }
    }
    
    /**
     * Create new role
     */
    suspend fun createRole(role: RoleRequest) {
        println("[$TAG] Creating role: ${role.roleName}")
        try {
            client.post("$baseUrl/roles") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(role)
            }
        } catch (e: Exception) {
            println("[$TAG] Error creating role: ${e.message}")
            throw e
        }
    }
    
    /**
     * Update role
     */
    suspend fun updateRole(roleId: Long, role: RoleRequest) {
        println("[$TAG] Updating role: $roleId")
        try {
            client.put("$baseUrl/roles/$roleId") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(role)
            }
        } catch (e: Exception) {
            println("[$TAG] Error updating role: ${e.message}")
            throw e
        }
    }
    
    // ==================== Menu-Role APIs ====================
    
    /**
     * Get all active menus by role ID
     */
    suspend fun getMenuRolesByRoleId(roleId: Long): List<MenuRoleResponse> {
        println("[$TAG] Getting menu-roles for role: $roleId")
        return try {
            client.get("$baseUrl/menu-role/$roleId") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting menu-roles: ${e.message}")
            throw e
        }
    }
    
    /**
     * Update menu-role assignments
     */
    suspend fun updateMenuRoles(menuRoles: List<MenuRoleRequest>): Map<String, Any> {
        println("[$TAG] Updating menu-roles: ${menuRoles.size} items")
        return try {
            client.put("$baseUrl/menu-role") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(menuRoles)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error updating menu-roles: ${e.message}")
            throw e
        }
    }
    
    // ==================== Permission APIs ====================
    
    /**
     * Get all permissions grouped
     */
    suspend fun getAllPermissions(): Map<String, List<PermissionResponse>> {
        println("[$TAG] Getting all permissions")
        return try {
            client.get("$baseUrl/permission") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting permissions: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get permission by ID
     */
    suspend fun getPermissionById(permissionId: Long): PermissionResponse {
        println("[$TAG] Getting permission: $permissionId")
        return try {
            client.get("$baseUrl/permission/$permissionId") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting permission: ${e.message}")
            throw e
        }
    }
    
    /**
     * Create permission type
     */
    suspend fun createPermission(permission: PermissionRequest): Map<String, Any> {
        println("[$TAG] Creating permission: ${permission.groupName}")
        return try {
            client.post("$baseUrl/permission") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(permission)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error creating permission: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get permissions for current user
     */
    suspend fun getPermissionsForUser(): Set<String> {
        println("[$TAG] Getting permissions for user")
        return try {
            client.get("$baseUrl/permission/user") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting user permissions: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get permission-roles by role ID
     */
    suspend fun getPermissionRolesByRoleId(roleId: Long): List<PermissionRoleResponse> {
        println("[$TAG] Getting permission-roles for role: $roleId")
        return try {
            client.get("$baseUrl/permission/role/$roleId") { auth() }.body()
        } catch (e: Exception) {
            println("[$TAG] Error getting permission-roles: ${e.message}")
            throw e
        }
    }
    
    /**
     * Update permission-role assignments
     */
    suspend fun updatePermissionRoles(permissionRoles: List<PermissionRoleRequest>): Map<String, Any> {
        println("[$TAG] Updating permission-roles: ${permissionRoles.size} items")
        return try {
            client.put("$baseUrl/permission/role") {
                auth()
                contentType(ContentType.Application.Json)
                setBody(permissionRoles)
            }.body()
        } catch (e: Exception) {
            println("[$TAG] Error updating permission-roles: ${e.message}")
            throw e
        }
    }
}
