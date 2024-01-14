package space.astro.bot.events.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent
import space.astro.shared.core.daos.ConfigurationErrorDao

@Component
class ConfigurationErrorEventListener(
    private val configurationErrorDao: ConfigurationErrorDao
) {

    @EventListener
    fun configurationErrorReceived(event: ConfigurationErrorEvent) {
        configurationErrorDao.save(event.guildId, event.configurationErrorData)
    }
}