package space.astro.bot.components

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.WebhookType
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.Route
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.requests.RestActionImpl
import org.springframework.stereotype.Component
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.properties.UpdatesChannelProperties
import space.astro.bot.ui.Links
import space.astro.bot.ui.Messages
import space.astro.bot.utils.extensions.iconFromUrl
import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData
import java.io.IOException
import java.util.Locale

private val log = KotlinLogging.logger { }

/**
 * Creates, changes and removes the channel where Astro posts announcements, status updates and the alerts of a guild.
 *
 * The channel follows the official announcement channels and has a webhook used only to push the alerts of the guild.
 */
@Component
class UpdatesChannelManager(
    private val updatesChannelProperties: UpdatesChannelProperties,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {
    companion object {
        const val CHANNEL_NAME = "astro-updates"
        private const val WEBHOOK_NAME = "Astro Alerts"

        // Manage Webhooks is last so the others are still given when it is missing
        private val CHANNEL_PERMISSIONS =
            listOf(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_HISTORY,
                Permission.MANAGE_WEBHOOKS,
            )
    }

    /**
     * Creates a new channel hidden from everyone and configures it, if the webhooks cannot be set up the channel is kept
     * without them
     */
    suspend fun createAndConfigure(
        guild: Guild,
        category: Category?,
        locale: Locale,
        reason: String,
        current: UpdatesChannelData? = null,
    ): UpdatesChannelData {
        val self = guild.selfMember
        // Discord only accepts overwrites with permissions the bot already has in the guild
        val allowed = Permission.getRaw(CHANNEL_PERMISSIONS.filter { self.hasPermission(it) })
        val channel =
            guild
                .createTextChannel(CHANNEL_NAME)
                .setParent(category)
                .addRolePermissionOverride(guild.publicRole.idLong, 0, Permission.VIEW_CHANNEL.rawValue)
                .addMemberPermissionOverride(self.idLong, allowed, 0)
                .reason(reason)
                .await()

        return try {
            configure(guild, channel, locale, current)
        } catch (e: ErrorResponseException) {
            log.warn(e) { "Failed to configure the new updates channel ${channel.id} of guild ${guild.id}" }
            recordError(
                guild.id,
                ConfigurationErrorCode.UNKNOWN,
                mapOf("encounteredIn" to "updates channel", "details" to (e.message ?: "")),
            )
            deleteWebhooks(guild.jda, current.webhookIds())

            UpdatesChannelData(channelId = channel.id)
        } catch (e: InsufficientPermissionException) {
            recordError(
                guild.id,
                ConfigurationErrorCode.MISSING_PERMISSION,
                mapOf("permission" to e.permission.getName(), "channelId" to channel.id),
            )
            deleteWebhooks(guild.jda, current.webhookIds())

            UpdatesChannelData(channelId = channel.id)
        }
    }

    /**
     * Sets up [channel] and only after that removes the webhooks of the [current] data that are not used anymore,
     * so if something fails halfway the previous setup keeps working
     *
     * @throws InsufficientPermissionException if Astro can't send messages in [channel]
     * @throws ErrorResponseException if Discord refuses to send the message, create the webhooks or follow the channels
     */
    suspend fun configure(
        guild: Guild,
        channel: TextChannel,
        locale: Locale,
        current: UpdatesChannelData?,
    ): UpdatesChannelData {
        val self = guild.selfMember

        if (!self.hasPermission(channel, Permission.VIEW_CHANNEL)) {
            throw InsufficientPermissionException(channel, Permission.VIEW_CHANNEL)
        }
        if (!self.hasPermission(channel, Permission.MESSAGE_SEND)) {
            throw InsufficientPermissionException(channel, Permission.MESSAGE_SEND)
        }

        channel.sendMessage(Messages.updatesChannelReady(guild, locale)).await()

        val updated =
            if (self.hasPermission(channel, Permission.MANAGE_WEBHOOKS)) {
                setUpWebhooks(guild, channel, current)
            } else {
                recordError(
                    guild.id,
                    ConfigurationErrorCode.MISSING_PERMISSION,
                    mapOf("permission" to Permission.MANAGE_WEBHOOKS.getName(), "channelId" to channel.id),
                )

                UpdatesChannelData(channelId = channel.id)
            }

        val keep = updated.webhookIds().toSet()
        deleteWebhooks(guild.jda, current.webhookIds().filter { it !in keep })

        return updated
    }

    /**
     * Deletes the webhooks, the channel is kept since it may contain messages the admins want
     */
    suspend fun disable(
        guild: Guild,
        current: UpdatesChannelData?,
    ) = deleteWebhooks(guild.jda, current.webhookIds())

    /**
     * Creates the alert webhook and follows the official channels that are not followed yet,
     * what has been created is deleted again if something fails
     */
    private suspend fun setUpWebhooks(
        guild: Guild,
        channel: TextChannel,
        current: UpdatesChannelData?,
    ): UpdatesChannelData {
        val jda = guild.jda
        // followers created by a previous setup of this same channel (a retry) are still ours
        val ownedFollowerIds = current?.takeIf { it.channelId == channel.id }?.followerWebhookIds.orEmpty()
        val existingFollowers = channel.retrieveWebhooks().await().filter { it.type == WebhookType.FOLLOWER }

        val created = mutableListOf<String>()

        try {
            val webhook =
                channel
                    .createWebhook(WEBHOOK_NAME)
                    .apply {
                        try {
                            setAvatar(iconFromUrl(Links.LOGO))
                        } catch (e: IOException) {
                            log.warn(e) { "Failed to download the avatar of the alert webhook" }
                        }
                    }.await()
            created += webhook.id

            val followerIds = mutableListOf<String>()
            val sources = listOf(updatesChannelProperties.announcementsChannelId, updatesChannelProperties.statusChannelId)

            for (source in sources.filter { it.isNotBlank() }) {
                val existing = existingFollowers.firstOrNull { it.sourceChannel?.id == source }

                // the admin may already follow the channel by hand, following it again would post every message twice
                if (existing != null) {
                    if (existing.id in ownedFollowerIds) followerIds += existing.id
                    continue
                }

                // not done with NewsChannel.follow because the official server may be on another shard and so not cached
                val followerId =
                    RestActionImpl(
                        jda,
                        Route.Channels.FOLLOW_CHANNEL.compile(source),
                        DataObject.empty().put("webhook_channel_id", channel.id),
                    ) { response, _ -> response.getObject().getString("webhook_id") }.await()
                created += followerId
                followerIds += followerId
            }

            return UpdatesChannelData(
                channelId = channel.id,
                alertWebhookId = webhook.id,
                alertWebhookToken = webhook.token,
                followerWebhookIds = followerIds,
            )
        } catch (e: Exception) {
            deleteWebhooks(jda, created)
            throw e
        }
    }

    private suspend fun deleteWebhooks(
        jda: JDA,
        ids: Collection<String>,
    ) {
        for (id in ids) {
            try {
                jda.retrieveWebhookById(id).flatMap { it.delete() }.await()
            } catch (e: ErrorResponseException) {
                if (e.errorResponse != ErrorResponse.UNKNOWN_WEBHOOK) {
                    log.warn(e) { "Failed to delete the webhook $id of an updates channel" }
                }
            }
        }
    }

    private fun recordError(
        guildId: String,
        code: ConfigurationErrorCode,
        args: Map<String, String>,
    ) = configurationErrorEventPublisher.publishConfigurationErrorEvent(
        ConfigurationErrorEntity(guildId = guildId, code = code, args = args),
    )

    private fun UpdatesChannelData?.webhookIds(): List<String> =
        this?.let { listOfNotNull(it.alertWebhookId) + it.followerWebhookIds }.orEmpty()
}
