package space.astro.bot.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.listeners.events.ConfigurationErrorEvent

@Component
class ConfigurationErrorListener {

    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        TODO()
    }
}