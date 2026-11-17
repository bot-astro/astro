package space.astro.shared.core.models.api.bot.request

/**
 * @param channelId the channel to use, null disables the updates channel
 * @param createChannel creates a new channel and uses it, [channelId] is ignored
 */
data class UpdatesChannelUpdateBotApiRequest(
    val channelId: String?,
    val createChannel: Boolean = false,
)
