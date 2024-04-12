package space.astro.api.central.models

import space.astro.api.central.models.discord.TokenPayloadWithOptionalGuildDto
import space.astro.shared.core.models.discord.DiscordUserData

data class AuthorizationWrapperDto(
    val user: DiscordUserData,
    val token: TokenPayloadWithOptionalGuildDto,
)
