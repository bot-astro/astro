package space.astro.bot.events.publishers

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.models.ConfigurationErrorEvent
import space.astro.shared.core.models.database.ConfigurationErrorEntity

@Component
class ConfigurationErrorEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishConfigurationErrorEvent(
        configurationErrorEntity: ConfigurationErrorEntity
    ) {
        applicationEventPublisher.publishEvent(ConfigurationErrorEvent(configurationErrorEntity))
    }
}