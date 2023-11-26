package space.astro.api.central.models

import space.astro.shared.core.models.discord.DiscordUserDto

data class AuthorizationWrapperDto(
    val user: DiscordUserDto,
    val token: TokenPayloadDto
)
