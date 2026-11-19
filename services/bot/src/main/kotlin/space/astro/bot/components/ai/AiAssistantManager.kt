package space.astro.bot.components.ai

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import space.astro.bot.ui.AiMessages
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import java.time.Duration

private val log = KotlinLogging.logger { }

@Component
class AiAssistantManager(
    private val client: AiAssistantClient,
    private val redis: StringRedisTemplate,
    private val premiumManager: PremiumManager,
) {
    private companion object {
        // threads are archived by Discord after a day of inactivity but anyone can reopen them, the marker outlives that
        val THREAD_TTL: Duration = Duration.ofDays(7)

        fun threadKey(threadId: String) = "ai:thread:$threadId"
    }

    /**
     * Marks [threadId] as a thread where mentioning the bot talks to the assistant, also extends the marker already there
     */
    fun markThread(threadId: String) {
        redis.opsForValue().set(threadKey(threadId), "1", THREAD_TTL)
    }

    fun isThread(threadId: String): Boolean = redis.hasKey(threadKey(threadId))

    /**
     * Runs a turn of the assistant and builds the message to show, failures become an error message
     *
     * @param quote what the user wrote, shown at the top when the message is not visible in the chat (e.g. it came from a modal)
     */
    suspend fun respond(
        guildId: String,
        userId: String,
        threadId: String,
        text: String,
        guildSettings: GuildSettingsEntity?,
        quote: String? = null,
    ): MessageCreateData {
        val locale = guildSettings.supportedLocale
        markThread(threadId)

        return try {
            val response = withContext(Dispatchers.IO) { client.turn(guildId, AiTurnRequest(userId, threadId, text)) }
            val premium = guildSettings?.let { premiumManager.isGuildPremium(it) } == true

            AiMessages.reply(locale, response, premium, quote)
        } catch (e: HttpClientErrorException.Forbidden) {
            AiMessages.error(locale, "ai.error.forbidden")
        } catch (e: RestClientException) {
            log.warn(e) { "AI assistant turn failed in guild $guildId" }
            AiMessages.error(locale, "ai.error.unavailable")
        }
    }
}
