package space.astro.bot.events.listeners.error

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.events.models.ConfigurationErrorEvent
import space.astro.shared.core.repositories.ConfigurationErrorRepository

@Component
class ConfigurationErrorEventListener(
    private val configurationErrorRepository: ConfigurationErrorRepository
) {

    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        configurationErrorRepository.save(event.configurationErrorEntity)
    }
}