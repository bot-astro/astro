package space.astro.api.central.models

import space.astro.shared.core.models.discord.DiscordUserDto

data class OAuth2AuthorizationResponseDto(
    val token: String,
    val user: DiscordUserDto
)
