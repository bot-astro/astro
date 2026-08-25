package space.astro.api.central.models

import space.astro.shared.core.models.discord.DiscordTokenPayload
import space.astro.shared.core.models.discord.DiscordUserDto

data class AuthUserAndAccessTokenWrapperDto(
    val user: DiscordUserDto,
    val token: DiscordTokenPayload,
)
