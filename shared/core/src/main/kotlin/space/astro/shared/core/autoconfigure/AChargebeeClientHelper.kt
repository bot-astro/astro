package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.ChargebeeClientHelper
import space.astro.shared.core.config.ChargebeeClientConfig

@AutoConfiguration
@EnableConfigurationProperties(ChargebeeClientConfig::class)
@ConditionalOnProperty(
    prefix = "chargebee",
    name = ["enabled"],
    havingValue = "true",
)
class AChargebeeClientHelper {

    @Bean
    @ConditionalOnMissingBean
    fun chargebeeClientHelper(
        config: ChargebeeClientConfig
    ) = ChargebeeClientHelper(config)
}