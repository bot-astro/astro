package space.astro.api.central.models.auth

data class AuthSession(
    val sessionId: String,
    val userId: String,
    val expiresAt: Long,
)
