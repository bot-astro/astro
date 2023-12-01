package space.astro.shared.core.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "bigquery")
class BigQueryConfig {

    var enabled = false
    var datasetName = "astro_local"
    var poolSize = 8
    var interval: Long = 15
    var delay: Long = 10
    var maxEventsEntry = 10_000
}
