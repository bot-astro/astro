package space.astro.api.central.models.discord

import space.astro.shared.core.models.discord.DiscordUserData

data class OAuth2AuthorizationResponseDto(
    val token: String,
    val user: DiscordUserData,
    val guild: OAuth2GuildInfo?
)
