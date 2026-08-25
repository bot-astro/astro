package space.astro.api.central.models.discord

import space.astro.shared.core.models.discord.DiscordUserDto

data class OAuth2AuthorizationResponseDto(
    val token: String,
    val user: DiscordUserDto,
    val guild: OAuth2GuildInfo?
)
