package space.astro.shared.core.models.analytics

data class SlashCommandInvocationEventData(
    val name: String,
    val guildId: Long,
    val channelId: Long,
    val userId: Long,
    val mainOptionName: String?,
    val mainOptionValue: String?,
    var rawOptions: String?,
    val timestamp: String
) : AnalyticsEventData
