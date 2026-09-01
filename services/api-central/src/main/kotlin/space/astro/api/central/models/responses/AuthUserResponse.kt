package space.astro.api.central.models.responses

import space.astro.shared.core.models.database.UserEntity

data class AuthUserResponse(
    val id: String,
    val username: String,
    val discriminator: String,
    val avatar: String?,
    val email: String?
)
