package space.astro.bot.events.listeners.entitlements

import net.dv8tion.jda.api.events.entitlement.EntitlementUpdateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EntitlementUpdateEventListener {

    @EventListener
    fun receiveEntitlementUpdateEvent(event: EntitlementUpdateEvent) {
        // TODO: Update event data
    }
}