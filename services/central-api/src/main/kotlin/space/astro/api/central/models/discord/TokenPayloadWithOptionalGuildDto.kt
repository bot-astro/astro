package space.astro.api.central.models.discord

data class TokenPayloadWithOptionalGuildDto(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String?,
    val tokenType: String?,
    val guild: TokenGuildPayloadDto?
) {
    fun asTokenPayloadDto() = TokenPayloadDto(accessToken, expiresIn, refreshToken, scope, tokenType)
}

data class TokenPayloadDto(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val scope: String?,
    val tokenType: String?,
)

data class TokenGuildPayloadDto(
    val id: String,
    val name: String,
    val icon: String?
)