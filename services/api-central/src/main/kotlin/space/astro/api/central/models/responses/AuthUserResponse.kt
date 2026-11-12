package space.astro.api.central.models.responses

data class AuthUserResponse(
    val id: String,
    val username: String,
    val avatar: String?,
)
