package space.astro.bot.models.analytics

/**
 * Data of an analytics event, published as a spring event and pushed to BigQuery
 *
 * Field names are the columns of the BigQuery table of the event
 */
sealed interface AnalyticsEventData

data class GuildEventData(
    val guildId: Long,
    val usersCount: Int,
    val locale: String?,
    val action: GuildEventAction,
    val timestamp: String,
) : AnalyticsEventData {
    enum class GuildEventAction {
        JOINED,
        KICKED,
    }
}

data class SlashCommandInvocationEventData(
    val name: String,
    val guildId: Long,
    val channelId: Long,
    val userId: Long,
    val mainOptionName: String?,
    val mainOptionValue: String?,
    val rawOptions: String?,
    val timestamp: String,
) : AnalyticsEventData

data class TemporaryVCGenerationEventData(
    val guildId: Long,
    val userId: Long,
    val generatorId: Long,
    val timestamp: String,
) : AnalyticsEventData

data class ConnectionInvocationEventData(
    val guildId: Long,
    val userId: Long,
    val connectionId: Long,
    val timestamp: String,
) : AnalyticsEventData

data class ConfigurationErrorEventData(
    val guildId: Long,
    val code: String,
    val timestamp: String,
) : AnalyticsEventData

/**
 * Daily picture of the configuration of a guild, used to follow premium status and setup depth over time
 */
data class GuildConfigSnapshotEventData(
    val guildId: Long,
    val premium: Boolean,
    val generators: Int,
    val interfaces: Int,
    val connections: Int,
    val templates: Int,
    val timestamp: String,
) : AnalyticsEventData
