package space.astro.shared.core.services.discord

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.entitlement.Entitlement
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.WebClientConfig
import java.time.Duration
import java.util.*

private val log = KotlinLogging.logger { }

@Service
class DiscordEntitlementsFetchService(
    webClientConfig: WebClientConfig,
    discordConfig: DiscordConfig,
    private val objectMapper: ObjectMapper
) {
    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("discord-entitlements-fetch-service-provider")
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


    /**
     * Fetches the entitlements for the [applicationId]
     *
     * @param applicationId The id of the bot application of which you need to fetch the entitlements
     * @param authToken Authentication token for the [applicationId]
     * @param guildId Optional guild id filter
     * @param userId Optional user id filter
     */
    suspend fun fetchEntitlements(
        applicationId: String,
        authToken: String,
        guildId: String?,
        userId: String?,
    ): List<Entitlement> {
        log.info("Fetching entitlements")

        try {
            return webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .pathSegment("applications", applicationId, "entitlements")
                        .queryParamIfPresent("guild_id", guildId?.let { Optional.of(it) } ?: Optional.empty<String>())
                        .queryParamIfPresent("user_id", userId?.let { Optional.of(it) } ?: Optional.empty<String>())
                        .build()
                }
                .header("Authorization", "Bot $authToken")
                .retrieve()
                .onStatus(
                    { it != HttpStatus.OK },
                    { throw Throwable("${it.statusCode()} - Unexpected Response") })
                .awaitBody<List<Entitlement>>()
        } catch (t: Throwable) {
            throw RuntimeException(
                "Unable to fetch entitlements for bot with id $applicationId!",
                t
            )
        }
    }
}