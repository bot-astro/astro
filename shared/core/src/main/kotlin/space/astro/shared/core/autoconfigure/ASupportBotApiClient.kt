package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.SupportBotApiClient
import space.astro.shared.core.properties.support_bot.SupportBotEndpointProperties

@AutoConfiguration
@EnableConfigurationProperties(SupportBotEndpointProperties::class)
@ConditionalOnProperty(
    prefix = "support.bot.endpoint",
    name = ["enabled"],
    havingValue = "true",
)
class ASupportBotApiClient {
    @Bean
    @ConditionalOnMissingBean
    fun supportBotApiClient(supportBotEndpointProperties: SupportBotEndpointProperties) =
        SupportBotApiClient(supportBotEndpointProperties)
}
