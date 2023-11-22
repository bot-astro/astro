package space.astro.bot.listeners.entitlements

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EntitlementUpdateEventListener {

    @EventListener
    fun receiveEntitlementUpdateEvent(event: EntitlementUpdateEvent) {
        // TODO: Update event data
    }
}