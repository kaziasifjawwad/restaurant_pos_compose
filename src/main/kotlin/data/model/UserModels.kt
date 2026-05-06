package data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserRegistrationRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String = "",
    val mobileNumber: String,
    val presetAddress: String = "",
    val permanentAddress: String = "",
    val roleId: List<Long> = emptyList()
)

@Serializable
data class UserRegistrationResponse(
    val email: String = "",
    val message: String = ""
)

@Serializable
data class EmailLookupResponse(
    val email: String = "",
    val exists: Boolean = false,
    val message: String = ""
)

@Serializable
data class ApiErrorResponse(
    val timestamp: String = "",
    val message: String = "",
    val path: String = "",
    val errorCode: String? = null
)

@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0
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
    val lastName: String = "",
    val mobileNumber: String,
    val presetAddress: String = "",
    val permanentAddress: String = "",
    val roleId: List<Long> = emptyList()
)
