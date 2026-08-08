package space.astro.shared.core.models.analytics

data class GuildSnapshotData(
    val guildId: Long,
    val guildName: String,
    val memberCount: Int,
    val shardId: Int,
    val timestamp: String
) : AnalyticsEventData