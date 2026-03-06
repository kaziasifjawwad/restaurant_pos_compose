package data.model

import kotlinx.serialization.Serializable

// ==================== User Registration Models ====================

/**
 * POST /user/registration
 * Fields mirror RegistrationRequest.java on the backend.
 */
@Serializable
data class UserRegistrationRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val presetAddress: String,
    val permanentAddress: String,
    val roleId: List<Long> = emptyList()
)

/**
 * 201 success body:
 * { "email": "...", "message": "successfully created user with username : ..." }
 */
@Serializable
data class UserRegistrationResponse(
    val email: String = "",
    val message: String = ""
)

/**
 * 4xx / 5xx error shape from the backend:
 * { "timestamp": "...", "message": "...", "path": "..." }
 */
@Serializable
data class ApiErrorResponse(
    val timestamp: String = "",
    val message: String = "",
    val path: String = ""
)

// ==================== User Management Models ====================

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int
)

@Serializable
data class UserInformationResponse(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val username: String = "",
    val presetAddress: String? = null,
    val permanentAddress: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class UserInformationResponseWithRole(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val username: String = "",
    val presetAddress: String? = null,
    val permanentAddress: String? = null,
    val userRoles: List<RoleResponse> = emptyList(),
    val enabled: Boolean = true
)

@Serializable
data class UserUpdateRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val presetAddress: String,
    val permanentAddress: String,
    val roleId: List<Long> = emptyList()
)
