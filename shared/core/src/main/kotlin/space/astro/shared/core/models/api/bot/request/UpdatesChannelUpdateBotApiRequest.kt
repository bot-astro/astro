package space.astro.shared.core.models.api.bot.request

import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData

/**
 * @param channelId the channel to use, null disables the updates channel
 * @param createChannel creates a new channel and uses it, [channelId] is ignored
 * @param current the data currently saved, used to clean up the webhooks of the previous channel
 */
data class UpdatesChannelUpdateBotApiRequest(
    val channelId: String?,
    val createChannel: Boolean = false,
    val current: UpdatesChannelData?,
)
