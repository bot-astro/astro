package space.astro.shared.core.models.api.bot.response

import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData

/**
 * @param updatesChannel null if the updates channel has been disabled
 */
data class UpdatesChannelBotApiResponse(
    val updatesChannel: UpdatesChannelData?,
)
