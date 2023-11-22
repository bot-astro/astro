package space.astro.bot.listeners.entitlements

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EntitlementDeleteEventListener {

    @EventListener
    fun receiveEntitlementDeleteEvent(event: EntitlementDeleteEvent) {
        // TODO: remove from guild data
    }
}