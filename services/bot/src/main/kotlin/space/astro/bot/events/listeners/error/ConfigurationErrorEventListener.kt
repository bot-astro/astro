package space.astro.bot.events.listeners.error

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.events.models.ConfigurationErrorEvent
import space.astro.bot.models.analytics.ConfigurationErrorEventData
import space.astro.shared.core.repositories.mongo.ConfigurationErrorRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class ConfigurationErrorEventListener(
    private val configurationErrorRepository: ConfigurationErrorRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        val error = event.configurationErrorEntity
        configurationErrorRepository.save(error)

        applicationEventPublisher.publishEvent(
            ConfigurationErrorEventData(
                guildId = error.guildId.toLong(),
                code = error.code.name,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }
}
