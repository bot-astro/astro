package space.astro.bot.components.analytics

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.astro.bot.models.analytics.AnalyticsEventData
import space.astro.bot.models.analytics.ConnectionInvocationEventData
import space.astro.bot.models.analytics.GuildEventData
import space.astro.bot.models.analytics.SlashCommandInvocationEventData
import space.astro.bot.models.analytics.TemporaryVCGenerationEventData
import space.astro.bot.properties.BigQueryProperties
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

/**
 * Batches analytics events and inserts them in the BigQuery table of their type.
 * When BigQuery is disabled events are only logged.
 */
@Service
class BigQueryService(
    private val properties: BigQueryProperties,
    jsonMapper: JsonMapper,
) {
    // Dedicated mapper: the application one uses snake_case, the existing tables use the field names as they are
    private val bigQueryJsonMapper =
        jsonMapper
            .rebuild()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build()
    private val bigQuery: BigQuery? by lazy {
        try {
            BigQueryOptions.getDefaultInstance().service
        } catch (e: Exception) {
            log.error(e) { "Unable to create BigQuery client" }
            null
        }
    }
    private val pool = ConcurrentLinkedQueue<AnalyticsEventData>()
    private val executor = Executors.newSingleThreadExecutor()

    fun push(data: AnalyticsEventData) {
        if (!properties.enabled) {
            log.info { "Simulating event dispatch since BigQuery is turned off: ${bigQueryJsonMapper.writeValueAsString(data)}" }
            return
        }

        pool.add(data)
        if (pool.size >= properties.maxEventsEntry) {
            executor.submit { dispatch() }
        }
    }

    @Scheduled(
        initialDelayString = "\${bigquery.delay:10}",
        fixedDelayString = "\${bigquery.interval:15}",
        timeUnit = TimeUnit.SECONDS,
    )
    fun dispatch() {
        if (!properties.enabled || pool.isEmpty()) return

        val client = bigQuery ?: return
        val events = generateSequence { pool.poll() }.toList()

        events
            .groupBy { tableName(it) }
            .forEach { (table, tableEvents) ->
                try {
                    val request =
                        InsertAllRequest
                            .newBuilder(properties.datasetName, table)
                            .also { builder ->
                                tableEvents.forEach { builder.addRow(bigQueryJsonMapper.convertValue<Map<String, Any?>>(it)) }
                            }.build()

                    val response = client.insertAll(request)
                    if (response.hasErrors()) {
                        response.insertErrors.values.forEach { log.error { "Failed while inserting analytics event in $table: $it" } }
                    }
                } catch (e: Exception) {
                    log.error(e) { "Failed to dispatch ${tableEvents.size} analytics events to $table" }
                }
            }
    }

    private fun tableName(data: AnalyticsEventData) =
        when (data) {
            is GuildEventData -> "GUILD_EVENT"
            is SlashCommandInvocationEventData -> "SLASH_COMMAND_INVOCATION"
            is TemporaryVCGenerationEventData -> "TEMPORARY_VC_GENERATION"
            is ConnectionInvocationEventData -> "CONNECTION_INVOCATION"
        }
}
