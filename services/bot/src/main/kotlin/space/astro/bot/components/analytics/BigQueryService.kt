package space.astro.bot.components.analytics

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.astro.bot.models.analytics.AnalyticsEventData
import space.astro.bot.models.analytics.ConfigurationErrorEventData
import space.astro.bot.models.analytics.ConnectionInvocationEventData
import space.astro.bot.models.analytics.GuildConfigSnapshotEventData
import space.astro.bot.models.analytics.GuildEventData
import space.astro.bot.models.analytics.SlashCommandInvocationEventData
import space.astro.bot.models.analytics.TemporaryVCGenerationEventData
import space.astro.bot.properties.BigQueryProperties
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.convertValue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger { }

/**
 * Batches analytics events and inserts them in the BigQuery table of their type.
 * When BigQuery is disabled events are only logged.
 */
@Service
class BigQueryService(
    private val properties: BigQueryProperties,
    private val jsonMapper: JsonMapper,
) {
    private val bigQuery: BigQuery? by lazy {
        try {
            BigQueryOptions.getDefaultInstance().service
        } catch (e: Exception) {
            log.error(e) { "Unable to create BigQuery client" }
            null
        }
    }

    private companion object {
        const val MAX_ROWS_PER_REQUEST = 500
    }

    private val pool = ConcurrentLinkedQueue<AnalyticsEventData>()

    // ConcurrentLinkedQueue.size walks the whole queue, so the size is tracked separately
    private val poolSize = AtomicInteger()
    private val executor = Executors.newSingleThreadExecutor()

    fun push(data: AnalyticsEventData) {
        if (!properties.enabled) {
            log.info { "Simulating event dispatch since BigQuery is turned off: ${jsonMapper.writeValueAsString(data)}" }
            return
        }

        pool.add(data)
        // only the push that crosses the threshold schedules a dispatch, the others would queue useless tasks
        if (poolSize.incrementAndGet() == properties.maxEventsEntry) {
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
        poolSize.addAndGet(-events.size)

        // the daily snapshot adds one row per guild at once, BigQuery recommends at most 500 rows per request
        val batches =
            events
                .groupBy { tableName(it) }
                .flatMap { (table, tableEvents) -> tableEvents.chunked(MAX_ROWS_PER_REQUEST).map { table to it } }

        batches.forEach { (table, batch) ->
            try {
                val request =
                    InsertAllRequest
                        .newBuilder(properties.datasetName, table)
                        .also { builder ->
                            batch.forEach { builder.addRow(jsonMapper.convertValue<Map<String, Any?>>(it)) }
                        }.build()

                val response = client.insertAll(request)
                if (response.hasErrors()) {
                    response.insertErrors.values.forEach { log.error { "Failed while inserting analytics event in $table: $it" } }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to dispatch ${batch.size} analytics events to $table" }
            }
        }
    }

    private fun tableName(data: AnalyticsEventData) =
        when (data) {
            is GuildEventData -> "GUILD_EVENT"
            is SlashCommandInvocationEventData -> "SLASH_COMMAND_INVOCATION"
            is TemporaryVCGenerationEventData -> "TEMPORARY_VC_GENERATION"
            is ConnectionInvocationEventData -> "CONNECTION_INVOCATION"
            is ConfigurationErrorEventData -> "CONFIGURATION_ERROR"
            is GuildConfigSnapshotEventData -> "GUILD_CONFIG_SNAPSHOT"
        }
}
