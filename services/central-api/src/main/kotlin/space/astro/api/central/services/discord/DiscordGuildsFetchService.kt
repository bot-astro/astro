package space.astro.api.central.services.discord

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.api.central.models.discord.DiscordPartialChannel
import space.astro.api.central.models.discord.DiscordPartialGuild
import space.astro.api.central.models.discord.DiscordPartialRole
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.WebClientConfig
import java.time.Duration

@Service
class DiscordGuildsFetchService(
    webClientConfig: WebClientConfig,
    discordConfig: DiscordConfig,
    val objectMapper: ObjectMapper
) {
    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("discord-guilds-fetch-service-provider")
            //.maxConnections(webClientConfig.httpMaxConnections)
            .maxIdleTime(Duration.ofSeconds(webClientConfig.httpMaxIdleTime))
            .maxLifeTime(Duration.ofSeconds(webClientConfig.httpMaxLifeTime))
            //.pendingAcquireTimeout(Duration.ofSeconds(webClientConfig.httpPendingAcquireTimeout))
            .evictInBackground(Duration.ofSeconds(webClientConfig.httpEvictInBackground))
            .build()

    private val webClient: WebClient = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(HttpClient.create(provider)))
        .codecs { configurer ->
            val codecs = configurer.defaultCodecs()
            codecs.jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            codecs.jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
        }
        .baseUrl(discordConfig.baseUrl)
        .build()

    suspend fun fetchGuilds(
        accessToken: String
    ): List<DiscordPartialGuild> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.pathSegment("users", "@me", "guilds")
                    .build()
            }
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("Failed to fetch user guilds - status: ${it.statusCode()}") }
            )
            .awaitBody()
    }

    suspend fun fetchGuildChannels(
        accessToken: String,
        guildID: String
    ): List<DiscordPartialChannel> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.pathSegment("guilds", guildID, "channels")
                    .build()
            }
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("Failed to fetch guild channels - status: ${it.statusCode()}") }
            )
            .awaitBody()
    }

    suspend fun fetchGuildRoles(
        accessToken: String,
        guildID: String
    ): List<DiscordPartialRole> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.pathSegment("guilds", guildID, "roles")
                    .build()
            }
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("Failed to fetch guild roles - status: ${it.statusCode()}") }
            )
            .awaitBody()
    }
}