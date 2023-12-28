package space.astro.bot.events.listeners.entitlements

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.entitlement.EntitlementDeleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.services.SupportBotApiService

@Component
class EntitlementDeleteEventListener(
    val supportBotApiService: SupportBotApiService,
    val coroutineScope: CoroutineScope
) {

    @EventListener
    fun receiveEntitlementDeleteEvent(event: EntitlementDeleteEvent) {
        coroutineScope.launch {
            supportBotApiService.forwardDeleteEntitlementEvent(event.entitlement)
        }
        // TODO: remove from guild data
    }
}