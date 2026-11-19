package space.astro.bot.events.listeners.error

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.GuildAlertService
import space.astro.bot.events.models.ConfigurationErrorEvent
import space.astro.shared.core.models.analytics.ConfigurationErrorEventData
import space.astro.shared.core.repositories.mongo.ConfigurationErrorRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class ConfigurationErrorEventListener(
    private val configurationErrorRepository: ConfigurationErrorRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val guildAlertService: GuildAlertService,
) {
    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        val error = event.configurationErrorEntity
        configurationErrorRepository.save(error)
        guildAlertService.alertConfigurationError(error)

        applicationEventPublisher.publishEvent(
            ConfigurationErrorEventData(
                guildId = error.guildId.toLong(),
                code = error.code.name,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }
}
