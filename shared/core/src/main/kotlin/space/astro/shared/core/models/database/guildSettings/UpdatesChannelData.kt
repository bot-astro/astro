package space.astro.shared.core.models.database.guildSettings

/**
 * The channel where Astro posts its announcements, status updates and the alerts about the setup of a guild
 *
 * The webhook token is a secret, it must never be sent to the dashboard
 *
 * @param alertWebhookId Webhook used only to push the alerts of this guild, null if it couldn't be created or doesn't exist anymore
 * @param followerWebhookIds Webhooks created by following the official channels, kept to clean them up when the channel changes
 */
data class UpdatesChannelData(
    val channelId: String,
    val alertWebhookId: String? = null,
    val alertWebhookToken: String? = null,
    val followerWebhookIds: List<String> = emptyList(),
    val status: Status = Status.OK,
) {
    enum class Status {
        OK,
        MISSING_PERMISSION,
        WEBHOOK_MISSING,
    }
}
