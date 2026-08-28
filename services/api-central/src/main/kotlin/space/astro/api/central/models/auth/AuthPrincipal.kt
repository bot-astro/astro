package space.astro.api.central.models.auth

data class AuthPrincipal(
    val sessionId: String,
    val userId: String,
    val userDiscordToken: String
)