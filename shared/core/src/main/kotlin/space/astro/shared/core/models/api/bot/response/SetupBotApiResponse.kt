package space.astro.shared.core.models.api.bot.response

import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.InterfaceData

data class SetupBotApiResponse(
    val category: DiscordGuildChannelBotApiResponse,
    val interfaceChannel: DiscordGuildChannelBotApiResponse,
    val generatorChannel: DiscordGuildChannelBotApiResponse,
    val interfaceData: InterfaceData,
    val generatorSettings: GeneratorSettings,
)
