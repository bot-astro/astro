package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.TopGGClient
import space.astro.shared.core.properties.TopGGProperties

@AutoConfiguration
@EnableConfigurationProperties(TopGGProperties::class)
@ConditionalOnProperty(
    prefix = "topgg",
    name = ["enabled"],
    havingValue = "true",
)
class ATopGGClient {
    @Bean
    @ConditionalOnMissingBean
    fun topGGClient(topGGProperties: TopGGProperties) = TopGGClient(topGGProperties)
}
