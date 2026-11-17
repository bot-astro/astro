package space.astro.api.central.models.responses

import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData

/**
 * What the dashboard knows about the updates channel, the webhook data is never exposed
 *
 * Both fields are null when the updates channel is disabled
 */
data class UpdatesChannelResponse(
    val channelId: String?,
    val status: UpdatesChannelData.Status?,
) {
    companion object {
        fun from(updatesChannel: UpdatesChannelData?) = UpdatesChannelResponse(updatesChannel?.channelId, updatesChannel?.status)
    }
}
