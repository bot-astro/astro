package space.astro.bot.events.publishers

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent
import space.astro.bot.models.error.ConfigurationError

@Component
class ConfigurationErrorEventPublisher(
    val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishConfigurationErrorEvent(
        guildId:String,
        configurationError: ConfigurationError
    ) {
        applicationEventPublisher.publishEvent(ConfigurationErrorEvent(guildId, configurationError))
    }
}