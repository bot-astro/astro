package space.astro.support.bot.components.jda

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class JdaToSpringEventBridge(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventListener {

    override fun onEvent(event: GenericEvent) {
        applicationEventPublisher.publishEvent(event)
    }

}
