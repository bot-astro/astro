package space.astro.shared.core.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.properties.DiscordApiProperties

@AutoConfiguration
@EnableConfigurationProperties(DiscordApiProperties::class)
@ConditionalOnProperty(
    prefix = "discord.api",
    value = ["enabled"],
    havingValue = "true",
)
class ADiscordApiClient {

    @Bean
    @ConditionalOnMissingBean
    fun discordApiClient(
        discordApiConfig: DiscordApiProperties,
    ): DiscordApiClient = DiscordApiClient(discordApiConfig)
}