package space.astro.bot.events.listeners.entitlements

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.entitlement.EntitlementUpdateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.services.SupportBotApiService

@Component
class EntitlementUpdateEventListener(
    val supportBotApiService: SupportBotApiService,
    val coroutineScope: CoroutineScope
) {

    @EventListener
    fun receiveEntitlementUpdateEvent(event: EntitlementUpdateEvent) {
        coroutineScope.launch {
            supportBotApiService.forwardCreateEntitlementEvent(event.entitlement)
        }
        // TODO: Update event data
    }
}