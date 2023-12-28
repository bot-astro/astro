package space.astro.bot.services.analytics

import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import space.astro.bot.services.analytics.bigquery.BigQueryService
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventData
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver

val log = KotlinLogging.logger { }

@Service
class AnalyticsService(
    val bigQueryService: BigQueryService
) {

    @EventListener
    fun receiveAnalyticsEvent(analyticsEvent: AnalyticsEvent) {
        log.debug("Received AnalyticsEvent {}", analyticsEvent)

        for (receiver in analyticsEvent.receivers) {
            when (receiver) {
                AnalyticsEventReceiver.BIGQUERY -> publishToBigQuery(analyticsEvent.data)
                AnalyticsEventReceiver.CONSOLE -> publishToConsole(analyticsEvent)
                else -> {}
            }
        }
    }

    private fun publishToConsole(event: AnalyticsEvent) {
        log.info("Received event $event!")
    }

    private fun publishToBigQuery(data: AnalyticsEventData) {
        bigQueryService.push(data)
    }

}
