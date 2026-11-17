package space.astro.bot.components

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.WebhookClient
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.bot.ui.Messages
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.models.database.guildSettings.UpdatesChannelData
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Posts the alerts about the setup of a guild in its updates channel, through the alert webhook.
 *
 * Admins stop reading a channel that is noisy, so every alert is muted after being posted
 * and a guild can't get more than [MAX_ALERTS_PER_HOUR] alerts per hour.
 */
@Service
class GuildAlertService(
    private val shardManager: ShardManager,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val redis: StringRedisTemplate,
) {
    private companion object {
        val MUTE_DURATION: Duration = Duration.ofHours(6)

        // longer than the mute so the count survives until the alert is posted again
        val SUPPRESSED_TTL: Duration = Duration.ofHours(12)
        val RATE_WINDOW: Duration = Duration.ofHours(1)
        const val MAX_ALERTS_PER_HOUR = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun alertConfigurationError(error: ConfigurationErrorEntity) {
        scope.launch {
            try {
                alert(error)
            } catch (e: Exception) {
                // never alert about failing to alert, the error is still visible in the dashboard
                log.warn(e) { "Failed to send the alert for configuration error ${error.code} of guild ${error.guildId}" }
            }
        }
    }

    private suspend fun alert(error: ConfigurationErrorEntity) {
        val guildSettings = guildSettingsRepository.findByGuildID(error.guildId) ?: return
        val updatesChannel = guildSettings.updatesChannel ?: return
        if (updatesChannel.alertWebhookId == null || updatesChannel.alertWebhookToken == null) return

        val guildId = error.guildId
        val errorHash = hash(error)
        val muteKey = RedisKey.ALERT_MUTE(guildId, errorHash)
        val suppressedKey = RedisKey.ALERT_SUPPRESSED(guildId, errorHash)

        if (redis.opsForValue().setIfAbsent(muteKey, "1", MUTE_DURATION) != true) {
            countSuppressed(suppressedKey)
            return
        }

        val rateKey = RedisKey.ALERT_GUILD_RATE(guildId)
        val alertsThisHour = redis.opsForValue().increment(rateKey) ?: 1
        if (alertsThisHour == 1L) redis.expire(rateKey, RATE_WINDOW)

        if (alertsThisHour > MAX_ALERTS_PER_HOUR) {
            // not posted, so it must not stay muted
            redis.delete(muteKey)
            countSuppressed(suppressedKey)

            // a single summary, the alerts after it are dropped until the hour is over
            if (alertsThisHour == MAX_ALERTS_PER_HOUR + 1L) {
                send(guildSettings, updatesChannel, Messages.alertsHeldBack(guildId, guildSettings.supportedLocale))
            }
            return
        }

        val occurrences = 1 + (redis.opsForValue().getAndDelete(suppressedKey)?.toLongOrNull() ?: 0L)

        send(
            guildSettings,
            updatesChannel,
            Messages.configurationErrorAlert(guildId, guildSettings.supportedLocale, error, occurrences),
        )
    }

    private fun countSuppressed(suppressedKey: String) {
        redis.opsForValue().increment(suppressedKey)
        redis.expire(suppressedKey, SUPPRESSED_TTL)
    }

    private suspend fun send(
        guildSettings: GuildSettingsEntity,
        updatesChannel: UpdatesChannelData,
        message: MessageCreateData,
    ) {
        val webhookId = updatesChannel.alertWebhookId ?: return
        val webhookToken = updatesChannel.alertWebhookToken ?: return

        val jda =
            shardManager.getGuildById(guildSettings.guildID)?.jda
                ?: shardManager.shards.firstOrNull()
                ?: return

        try {
            WebhookClient.createClient(jda, webhookId, webhookToken).sendMessage(message).await()
        } catch (e: ErrorResponseException) {
            when (e.errorResponse) {
                ErrorResponse.UNKNOWN_WEBHOOK, ErrorResponse.UNKNOWN_CHANNEL, ErrorResponse.INVALID_WEBHOOK_TOKEN ->
                    markWebhookMissing(guildSettings.guildID, webhookId)

                // rate limits and everything else: the alert is dropped, the cap and the mute already bound how many are sent
                else -> log.debug(e) { "Alert dropped for guild ${guildSettings.guildID}" }
            }
        }
    }

    /**
     * The dashboard keeps showing the error and offers to set the webhook up again
     */
    private fun markWebhookMissing(
        guildId: String,
        webhookId: String,
    ) {
        val guildSettings = guildSettingsRepository.findByGuildID(guildId) ?: return
        val updatesChannel = guildSettings.updatesChannel ?: return

        // the webhook may have been replaced in the meantime
        if (updatesChannel.alertWebhookId != webhookId) return

        guildSettings.updatesChannel =
            updatesChannel.copy(
                alertWebhookId = null,
                alertWebhookToken = null,
                status = UpdatesChannelData.Status.WEBHOOK_MISSING,
            )
        guildSettingsRepository.save(guildSettings)
    }

    private fun hash(error: ConfigurationErrorEntity): String {
        val identity =
            error.code.name +
                error.args
                    .toSortedMap()
                    .entries
                    .joinToString("|") { "${it.key}=${it.value}" }
        return UUID.nameUUIDFromBytes(identity.toByteArray()).toString()
    }
}
