package space.astro.api.central.models.responses

import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

data class GuildSetupResponse(
    val guildSettings: GuildSettingsEntity,
    val category: DiscordGuildChannelBotApiResponse,
    val interfaceChannel: DiscordGuildChannelBotApiResponse,
    val generatorChannel: DiscordGuildChannelBotApiResponse,
)
