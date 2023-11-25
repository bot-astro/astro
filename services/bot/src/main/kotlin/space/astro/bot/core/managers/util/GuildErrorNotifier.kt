package space.astro.bot.core.managers.util

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import space.astro.bot.events.ConfigurationErrorEvent

@Component
class GuildErrorNotifier(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun send() {
        /*
        TODO: create an error dto with error type and data. The data is dynamic and can be deserialized into specific data types based on the error type
        then the data gets saved in influx
        or we can put a simple message instead of custom deserialization stuff
         */
        val event = ConfigurationErrorEvent()
        applicationEventPublisher.publishEvent(event)
    }
}