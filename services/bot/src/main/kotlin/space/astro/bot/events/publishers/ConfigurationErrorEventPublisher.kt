package space.astro.bot.events.publishers

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent
import space.astro.shared.core.models.influx.ConfigurationErrorData

@Component
class ConfigurationErrorEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishConfigurationErrorEvent(
        configurationErrorData: ConfigurationErrorData
    ) {
        applicationEventPublisher.publishEvent(ConfigurationErrorEvent(configurationErrorData))
    }
}