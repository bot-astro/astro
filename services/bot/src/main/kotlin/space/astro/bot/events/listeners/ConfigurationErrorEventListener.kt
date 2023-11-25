package space.astro.bot.events.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent

@Component
class ConfigurationErrorEventListener {

    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        TODO("Save to influx")
    }
}