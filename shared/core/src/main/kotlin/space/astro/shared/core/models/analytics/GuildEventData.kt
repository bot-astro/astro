package space.astro.shared.core.models.analytics

data class GuildEventData(
    val guildId: Long,
    val usersCount: Int,
    val action: GuildEventAction,
    val timestamp: String
) : AnalyticsEventData {
    enum class GuildEventAction {
        JOINED, KICKED
    }
}
