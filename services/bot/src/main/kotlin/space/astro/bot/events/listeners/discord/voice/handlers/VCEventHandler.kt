package space.astro.bot.events.listeners.discord.voice.handlers

import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.models.analytics.ConnectionInvocationEventData
import space.astro.bot.models.analytics.TemporaryVCGenerationEventData
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.utils.extensions.toConfigurationErrorDto
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Dispatches the [VCEvent]s to their handler, publishes the analytics and maps the errors
 */
@Component
class VCEventHandler(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val joinedGeneratorHandler: JoinedGeneratorHandler,
    private val joinedTemporaryVCHandler: JoinedTemporaryVCHandler,
    private val leftTemporaryVCHandler: LeftTemporaryVCHandler,
    private val connectedVCHandler: ConnectedVCHandler,
) {
    suspend fun handleEvents(
        events: List<VCEvent>,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        val generatorEvents = events.filterIsInstance<VCEvent.JoinedGenerator>()
        val nonGeneratorEvents = events.filter { it !is VCEvent.JoinedGenerator }.toMutableList()

        generatorEvents.forEach { event ->
            try {
                if (joinedGeneratorHandler.handle(event, memberRolesManager)) {
                    publishTemporaryVCGeneration(event)
                }
            } catch (e: Exception) {
                // Remove connection events that were related to this joined generator event
                nonGeneratorEvents.removeAll {
                    it is VCEvent.JoinedConnectedVC && it.connectionData.id == event.generatorSettings.id
                }

                handleException(event, e)
            }
        }

        nonGeneratorEvents.forEach { event ->
            try {
                when (event) {
                    is VCEvent.JoinedTemporaryVC -> joinedTemporaryVCHandler.handle(event)
                    is VCEvent.LeftTemporaryVC -> leftTemporaryVCHandler.handle(event, memberRolesManager)
                    is VCEvent.JoinedConnectedVC -> {
                        connectedVCHandler.handleJoined(event, memberRolesManager)
                        publishConnectionInvocation(event)
                    }
                    is VCEvent.LeftConnectedVC -> connectedVCHandler.handleLeft(event, memberRolesManager)
                    is VCEvent.JoinedGenerator -> error("Generator events are handled before the others")
                }
            } catch (e: Exception) {
                handleException(event, e)
            }
        }
    }

    private fun publishTemporaryVCGeneration(event: VCEvent.JoinedGenerator) {
        applicationEventPublisher.publishEvent(
            TemporaryVCGenerationEventData(
                guildId = event.vcEventData.guild.idLong,
                userId = event.vcEventData.member.idLong,
                generatorId = event.generatorSettings.id.toLong(),
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }

    private fun publishConnectionInvocation(event: VCEvent.JoinedConnectedVC) {
        applicationEventPublisher.publishEvent(
            ConnectionInvocationEventData(
                guildId = event.vcEventData.guild.idLong,
                userId = event.vcEventData.member.idLong,
                connectionId = event.connectionData.id.toLong(),
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }

    private fun handleException(
        event: VCEvent,
        e: Exception,
    ) {
        when (e) {
            is ConfigurationException ->
                configurationErrorEventPublisher.publishConfigurationErrorEvent(e.configurationErrorEntity)
            is InsufficientPermissionException ->
                configurationErrorEventPublisher.publishConfigurationErrorEvent(
                    e.toConfigurationErrorDto(event.vcEventData.guild.id),
                )
            else -> throw e
        }
    }
}
