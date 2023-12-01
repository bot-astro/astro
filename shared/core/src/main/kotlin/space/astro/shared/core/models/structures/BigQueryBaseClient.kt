package space.astro.shared.core.models.structures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryError
import com.google.cloud.bigquery.InsertAllRequest
import mu.KotlinLogging
import space.astro.shared.core.configs.BigQueryConfig
import space.astro.shared.core.models.analytics.AnalyticsEventData
import space.astro.shared.core.models.analytics.AnalyticsEventType
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.reflect.KClass

val log = KotlinLogging.logger {}

abstract class BigQueryBaseClient(
    private val tables: Map<KClass<*>, AnalyticsEventType>,
    private val bigQuery: BigQuery?,
    private val bigQueryConfig: BigQueryConfig,
    objectMapper: ObjectMapper
) {
    private val objectMapper: ObjectMapper
    private val eventsPool: MutableList<AnalyticsEventData> =
        Collections.synchronizedList(ArrayList())
    private val executor = Executors.newScheduledThreadPool(bigQueryConfig.poolSize)

    fun push(data: AnalyticsEventData) {
        if (bigQueryConfig.enabled && bigQuery != null) {
            eventsPool.add(data)
            if (eventsPool.size >= bigQueryConfig.maxEventsEntry) {
                executor.submit { dispatch() }
            }
        } else {
            val simulatedEvent = deserialize(data)
            log.info("Simulating event dispatch since BigQuery is turned off!\n$simulatedEvent")
        }
    }

    private fun scheduledPush() {
        dispatch()
    }

    private fun dispatch() {
        if (!bigQueryConfig.enabled || eventsPool.isEmpty()) {
            log.debug("BigQuery enabled: ${bigQueryConfig.enabled} - No events to dispatch: ${eventsPool.isEmpty()}")
            return
        }
        val clone: List<AnalyticsEventData> = ArrayList(eventsPool)
        eventsPool.clear()
        try {
            val sortedEventsData: MutableMap<AnalyticsEventType, MutableList<AnalyticsEventData>> =
                EnumMap(AnalyticsEventType::class.java)
            for (analyticsEventData in clone) {
                sortedEventsData.computeIfAbsent(
                    tables[analyticsEventData::class]!!
                ) { ArrayList() }.add(analyticsEventData)
            }
            sortedEventsData.forEach { (analyticsEventType: AnalyticsEventType, analyticsEventsData: List<AnalyticsEventData>) ->
                val builder = InsertAllRequest.newBuilder(
                    bigQueryConfig.datasetName, analyticsEventType.name
                )
                analyticsEventsData.stream()
                    .map { data: AnalyticsEventData ->
                        createRow(
                            data
                        )
                    }.forEach { content: Map<String, Any> ->
                        builder.addRow(
                            content
                        )
                    }
                val response = bigQuery!!.insertAll(builder.build())
                if (response.hasErrors()) {
                    response.insertErrors.values.forEach(Consumer { error: List<BigQueryError> ->
                        log.error("Failed while inserting analytics event: $error")
                    })
                } else {
                    log.debug("Successfully inserted analytics event to bigquery")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    private fun createRow(data: AnalyticsEventData): Map<String, Any> {
        return objectMapper.convertValue(data)
    }

    private fun deserialize(data: AnalyticsEventData): String {
        return objectMapper.writeValueAsString(data)
    }

    init {
        this.objectMapper = objectMapper
        executor.scheduleAtFixedRate(
            this::scheduledPush,
            bigQueryConfig.delay,
            bigQueryConfig.interval,
            TimeUnit.SECONDS
        )
    }
}
