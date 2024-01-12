package space.astro.bot.events.publishers

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent
import space.astro.shared.core.models.influx.ConfigurationErrorDto

@Component
class ConfigurationErrorEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishConfigurationErrorEvent(
        guildId:String,
        configurationErrorDto: ConfigurationErrorDto
    ) {
        applicationEventPublisher.publishEvent(ConfigurationErrorEvent(guildId, configurationErrorDto))
    }
}