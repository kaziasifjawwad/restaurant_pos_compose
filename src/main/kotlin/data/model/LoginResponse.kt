package data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val userName: String? = null,
    val jwtToken: String,
    val roles: List<String>? = null,
    val expiredDate: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobileNumber: String? = null,
    val presetAddress: String? = null,
    val permanentAddress: String? = null
)
