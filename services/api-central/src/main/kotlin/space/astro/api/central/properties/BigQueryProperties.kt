package space.astro.api.central.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bigquery")
data class BigQueryProperties(
    val enabled: Boolean = false,
    val datasetName: String = "astro_local",
)
