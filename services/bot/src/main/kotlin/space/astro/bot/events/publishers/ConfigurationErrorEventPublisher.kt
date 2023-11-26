package space.astro.bot.events.publishers

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent
import space.astro.bot.models.error.ConfigurationErrorDto

@Component
class ConfigurationErrorEventPublisher(
    val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publishConfigurationErrorEvent(
        guildId:String,
        configurationErrorDto: ConfigurationErrorDto
    ) {
        applicationEventPublisher.publishEvent(ConfigurationErrorEvent(guildId, configurationErrorDto))
    }
}