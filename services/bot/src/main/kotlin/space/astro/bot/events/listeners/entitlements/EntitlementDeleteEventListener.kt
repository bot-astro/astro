package space.astro.bot.events.listeners.entitlements

import net.dv8tion.jda.api.events.entitlement.EntitlementDeleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class EntitlementDeleteEventListener {

    @EventListener
    fun receiveEntitlementDeleteEvent(event: EntitlementDeleteEvent) {
        // TODO: remove from guild data
    }
}