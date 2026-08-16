package space.astro.shared.core.models.analytics

data class TemporaryVCGenerationEventData(
    val guildId: Long,
    val userId: Long,
    val generatorId: Long,
    val timestamp: String
) : AnalyticsEventData
