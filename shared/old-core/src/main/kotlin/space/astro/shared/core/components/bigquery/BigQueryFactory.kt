package space.astro.shared.core.components.bigquery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class BigQueryFactory {

    @Bean
    fun getBigQueryClient(): BigQuery? {
        return try {
            BigQueryOptions.getDefaultInstance().service
        } catch (e: Exception) {
            e.printStackTrace()
            log.error { "Unable to create BigQuery client ${e.message}" }
            null
        }
    }
}
