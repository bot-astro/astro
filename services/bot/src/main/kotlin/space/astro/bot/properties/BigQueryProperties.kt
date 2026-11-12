package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bigquery")
data class BigQueryProperties(
    val enabled: Boolean = false,
    val datasetName: String = "astro_local",
    /** Events in the pool that trigger a dispatch before the next scheduled one */
    val maxEventsEntry: Int = 10_000,
)
