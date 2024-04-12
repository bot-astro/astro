package space.astro.api.central.models.discord

data class DiscordAuthedUser(
    val id: String,
    val discordAuthTokenInfo: TokenPayloadDto,
)