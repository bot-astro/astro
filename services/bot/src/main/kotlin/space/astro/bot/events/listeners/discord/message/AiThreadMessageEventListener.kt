package space.astro.bot.events.listeners.discord.message

import dev.minn.jda.ktx.coroutines.await
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.ai.AiAssistantManager
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

private val log = KotlinLogging.logger { }

/**
 * Talks to the AI assistant when someone mentions the bot in one of its threads, the bot has no message content
 * intent but the content is delivered for the messages that mention it
 */
@Component
class AiThreadMessageEventListener(
    private val aiAssistantManager: AiAssistantManager,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || !event.isFromThread || event.author.isBot) {
            return
        }

        val selfUser = event.jda.selfUser
        if (!event.message.mentions.isMentioned(selfUser, Message.MentionType.USER)) {
            return
        }

        if (!aiAssistantManager.isThread(event.channel.id)) {
            return
        }

        val text =
            event.message.contentRaw
                .replace(Regex("<@!?${selfUser.id}>"), "")
                .trim()
        if (text.isEmpty()) {
            return
        }

        scope.launch {
            try {
                event.channel.sendTyping().queue()

                val guildSettings = withContext(Dispatchers.IO) { guildSettingsRepository.findByGuildID(event.guild.id) }
                val reply = aiAssistantManager.respond(event.guild.id, event.author.id, event.channel.id, text, guildSettings)

                event.message
                    .reply(reply)
                    .mentionRepliedUser(false)
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(e) { "Failed to answer a mention in the AI thread ${event.channel.id} of guild ${event.guild.id}" }
            }
        }
    }
}
