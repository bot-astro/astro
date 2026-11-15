package space.astro.bot.events.listeners.discord.voice.handlers

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
import space.astro.shared.core.models.database.ConfigurationErrorCode
import java.time.Duration
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
    private val registry: MeterRegistry,
) {
    private val creationTimer =
        Timer
            .builder("astro.vc.create.duration")
            .serviceLevelObjectives(
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5),
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
            ).register(registry)

    suspend fun handleEvents(
        events: List<VCEvent>,
        memberRolesManager: SimpleMemberRolesManager,
    ) {
        val generatorEvents = events.filterIsInstance<VCEvent.JoinedGenerator>()
        val nonGeneratorEvents = events.filter { it !is VCEvent.JoinedGenerator }.toMutableList()

        generatorEvents.forEach { event ->
            val sample = Timer.start(registry)
            // stays "failed" if an exception that is not handled below escapes
            var outcome = "failed"

            try {
                if (joinedGeneratorHandler.handle(event, memberRolesManager)) {
                    outcome = "created"
                    publishTemporaryVCGeneration(event)
                } else {
                    outcome = "skipped"
                }
            } catch (e: Exception) {
                outcome = generationOutcome(e)

                // Remove connection events that were related to this joined generator event
                nonGeneratorEvents.removeAll {
                    it is VCEvent.JoinedConnectedVC && it.connectionData.id == event.generatorSettings.id
                }

                handleException(event, e)
            } finally {
                Counter
                    .builder("astro.vc.created")
                    .tag("outcome", outcome)
                    .register(registry)
                    .increment()

                if (outcome == "created") sample.stop(creationTimer)
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

    // UNKNOWN configuration errors wrap Discord API failures while creating the channel, the other codes are guild misconfigurations
    private fun generationOutcome(e: Exception) =
        when {
            e is InsufficientPermissionException -> "no_permission"
            e is ConfigurationException && e.configurationErrorEntity.code != ConfigurationErrorCode.UNKNOWN -> "config_error"
            else -> "failed"
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
