package space.astro.bot.components.analytics

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import space.astro.shared.core.clients.BigQueryService
import space.astro.shared.core.models.analytics.AnalyticsEventData

@Service
class AnalyticsService(
    private val bigQueryService: BigQueryService,
) {
    @EventListener
    fun analyticsEventReceived(data: AnalyticsEventData) {
        bigQueryService.push(data)
    }
}
