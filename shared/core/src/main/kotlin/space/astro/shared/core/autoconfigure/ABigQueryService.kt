package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import space.astro.shared.core.clients.BigQueryService
import space.astro.shared.core.properties.BigQueryProperties
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(BigQueryProperties::class)
class ABigQueryService {
    @Bean
    @ConditionalOnMissingBean
    fun bigQueryService(
        properties: BigQueryProperties,
        jsonMapper: JsonMapper,
    ) = BigQueryService(properties, jsonMapper)
}
