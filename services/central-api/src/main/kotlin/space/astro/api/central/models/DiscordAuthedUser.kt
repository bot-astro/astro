package space.astro.api.central.models

data class DiscordAuthedUser(
    val id: String,
    val discordAuthTokenInfo: TokenPayloadDto,
)