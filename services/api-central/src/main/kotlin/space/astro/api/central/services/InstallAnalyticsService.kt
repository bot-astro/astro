package space.astro.api.central.services

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import space.astro.api.central.properties.BigQueryProperties
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors

private val log = KotlinLogging.logger { }

/**
 * Records in BigQuery which source led to the installation of the bot in a guild.
 * When BigQuery is disabled events are only logged.
 */
@Service
class InstallAnalyticsService(
    private val properties: BigQueryProperties,
) {
    private val bigQuery: BigQuery? by lazy {
        try {
            BigQueryOptions.getDefaultInstance().service
        } catch (e: Exception) {
            log.error(e) { "Unable to create BigQuery client" }
            null
        }
    }

    // installs are rare, so they are inserted one by one off the request thread
    private val executor = Executors.newSingleThreadExecutor()

    fun recordInstall(
        source: String,
        guildId: String,
        inviterId: String?,
    ) {
        val row =
            mapOf(
                "source" to source,
                "guild_id" to guildId.toLong(),
                "user_id" to inviterId?.toLong(),
                "timestamp" to OffsetDateTime.now(ZoneOffset.UTC).toString(),
            )

        if (!properties.enabled) {
            log.info { "Simulating install event dispatch since BigQuery is turned off: $row" }
            return
        }

        executor.submit {
            try {
                val client = bigQuery ?: return@submit
                val response = client.insertAll(InsertAllRequest.newBuilder(properties.datasetName, "INSTALL_EVENT").addRow(row).build())
                if (response.hasErrors()) {
                    response.insertErrors.values.forEach { log.error { "Failed while inserting install event: $it" } }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to dispatch the install event of guild $guildId" }
            }
        }
    }
}
