package space.astro.shared.core.models.api.bot.response

import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData

/**
 * @param updatesChannel the updates channel created by the setup, null if the guild already had one or Astro couldn't create it
 * @param updatesChannelData the data of [updatesChannel] to save, null in the same cases
 */
data class SetupBotApiResponse(
    val category: DiscordGuildChannelBotApiResponse,
    val interfaceChannel: DiscordGuildChannelBotApiResponse,
    val generatorChannel: DiscordGuildChannelBotApiResponse,
    val updatesChannel: DiscordGuildChannelBotApiResponse?,
    val updatesChannelData: UpdatesChannelData?,
    val interfaceData: InterfaceData,
    val generatorSettings: GeneratorSettings,
)
