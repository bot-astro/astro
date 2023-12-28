package space.astro.shared.core.models.analytics

data class AnalyticsEvent(

    val receivers: List<AnalyticsEventReceiver>,
    val type: AnalyticsEventType,
    val data: AnalyticsEventData
)
