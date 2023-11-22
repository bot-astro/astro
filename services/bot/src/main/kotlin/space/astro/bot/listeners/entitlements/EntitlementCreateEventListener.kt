package space.astro.bot.listeners.entitlements

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EntitlementCreateEventListener {
    @EventListener
    fun receiveEntitlementCreateEvent(event: EntitlementCreateEvent) {
        // TODO: Save entitlement in guild data
    }
}