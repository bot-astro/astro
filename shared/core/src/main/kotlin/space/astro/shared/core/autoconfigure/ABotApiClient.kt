package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.properties.bot.BotEndpointProperties
import space.astro.shared.core.properties.bot.BotShardProperties
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration
@EnableConfigurationProperties(BotEndpointProperties::class, BotShardProperties::class)
@ConditionalOnProperty(
    prefix = "bot.endpoint",
    name = ["localhost"],
)
class ABotApiClient {
    @Bean
    @ConditionalOnMissingBean
    fun botApiClient(
        botEndpointProperties: BotEndpointProperties,
        botShardProperties: BotShardProperties,
        jsonMapper: JsonMapper,
    ) = BotApiClient(botEndpointProperties, botShardProperties, jsonMapper)
}
