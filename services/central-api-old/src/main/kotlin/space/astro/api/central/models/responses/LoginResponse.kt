package space.astro.api.central.models.responses

import space.astro.shared.core.models.discord.DiscordTokenGuildDto
import space.astro.shared.core.models.discord.DiscordUserDto

data class LoginResponse(
    val discordUser: DiscordUserDto,
    val discordGuild: DiscordTokenGuildDto?
)
