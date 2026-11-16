package space.astro.bot.events.listeners.discord.channel

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateParentEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository

private val log = KotlinLogging.logger { }

/**
 * When a generator is moved to another category, the category of the temporary VCs, chats and waiting rooms
 * that pointed to its old category follow it
 */
@Component
class GeneratorMovedEventListener(
    private val guildSettingsRepository: GuildSettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun channelParentUpdated(event: ChannelUpdateParentEvent) {
        if (event.channelType != ChannelType.VOICE) {
            return
        }

        val guildId = event.guild.id
        val channelId = event.channel.id
        val oldCategoryId = event.oldParent?.id
        val newCategoryId = event.newParent?.id

        scope.launch {
            try {
                val guildSettings = guildSettingsRepository.findByGuildID(guildId) ?: return@launch
                val generator = guildSettings.generators.firstOrNull { it.id == channelId } ?: return@launch

                var changed = false

                if (generator.category == oldCategoryId) {
                    generator.category = newCategoryId
                    changed = true
                }

                if (generator.chatCategory == oldCategoryId) {
                    generator.chatCategory = newCategoryId
                    changed = true
                }

                if (generator.waitingCategory == oldCategoryId) {
                    generator.waitingCategory = newCategoryId
                    changed = true
                }

                if (changed) {
                    guildSettingsRepository.save(guildSettings)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn(e) { "Failed to move the categories of generator $channelId in guild $guildId" }
            }
        }
    }
}
