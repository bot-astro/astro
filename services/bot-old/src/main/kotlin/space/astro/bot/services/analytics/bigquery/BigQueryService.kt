package space.astro.bot.services.analytics.bigquery

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.BigQuery
import org.springframework.stereotype.Service
import space.astro.shared.core.configs.BigQueryConfig
import space.astro.shared.core.models.analytics.*
import space.astro.shared.core.models.structures.BigQueryBaseClient
import java.util.*
import kotlin.reflect.KClass

@Service
class BigQueryService(
    bigQuery: BigQuery?,
    bigQueryConfig: BigQueryConfig,
    objectMapper: ObjectMapper
) :
    BigQueryBaseClient(
        TABLES,
        bigQuery, bigQueryConfig, objectMapper
    ) {
    companion object {
        private val TABLES: Map<KClass<*>, AnalyticsEventType>

        init {
            val tables: Map<KClass<*>, AnalyticsEventType> = HashMap(
                mapOf(
                    GuildEventData::class to AnalyticsEventType.GUILD_EVENT,
                    SlashCommandInvocationEventData::class to AnalyticsEventType.SLASH_COMMAND_INVOCATION,
                    TemporaryVCGenerationEventData::class to AnalyticsEventType.TEMPORARY_VC_GENERATION,
                    ConnectionInvocationEventData::class to AnalyticsEventType.CONNECTION_INVOCATION
                )
            )
            TABLES = Collections.unmodifiableMap(tables)
        }
    }
}
