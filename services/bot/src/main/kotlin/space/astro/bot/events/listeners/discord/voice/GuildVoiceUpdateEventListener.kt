package space.astro.bot.events.listeners.discord.voice

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.events.listeners.discord.voice.handlers.VCEventHandler
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEventData
import space.astro.bot.utils.extensions.toConfigurationErrorDto
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

private val log = KotlinLogging.logger { }

@Component
class GuildVoiceUpdateEventListener(
    private val guildSettingsRepository: GuildSettingsRepository,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcEventDetector: VCEventDetector,
    private val vcEventHandler: VCEventHandler,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {
    // IO because the repositories are blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        // Ignore bots and when a user doesn't switch voice channel
        if (event.member.user.isBot || event.channelJoined?.id == event.channelLeft?.id) {
            return
        }

        scope.launch {
            try {
                handle(event)
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while handling a voice update in guild ${event.guild.id}" }
            }
        }
    }

    private suspend fun handle(event: GuildVoiceUpdateEvent) {
        val guildSettings = withContext(Dispatchers.IO) {
            guildSettingsRepository.findByGuildID(event.guild.id)
        } ?: return

        val vcEventData =
            VCEventData(
                event = event,
                guildSettings = guildSettings,
                temporaryVCEntities = withContext(Dispatchers.IO) { temporaryVCRepository.getOfGuild(event.guild.id) },
            )

        val events = vcEventDetector.detectAstroVCEvents(vcEventData)
        val memberRolesManager = SimpleMemberRolesManager(event.guild, event.member)

        vcEventHandler.handleEvents(events, memberRolesManager)

        try {
            memberRolesManager.queue()
        } catch (e: HierarchyException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(e.toConfigurationErrorDto(event.guild.id))
        } catch (e: InsufficientPermissionException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(e.toConfigurationErrorDto(event.guild.id))
        }
    }
}
