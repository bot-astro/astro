package space.astro.shared.core.models.analytics

data class ConnectionInvocationEventData(
    val guildId: Long,
    val userId: Long,
    val connectionId: Long,
    val timestamp: String
) : AnalyticsEventData
