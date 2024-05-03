package space.astro.api.central.models

import space.astro.api.central.models.discord.TokenPayloadWithOptionalGuildDto
import space.astro.shared.core.models.discord.DiscordUserDto

data class AuthUserAndAccessTokenWrapperDto(
    val user: DiscordUserDto,
    val token: TokenPayloadWithOptionalGuildDto,
)
