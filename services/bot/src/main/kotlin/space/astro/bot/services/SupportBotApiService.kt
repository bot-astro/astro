package space.astro.bot.services

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.entitlement.Entitlement
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.SupportBotApiConfig
import space.astro.shared.core.configs.WebClientConfig
import java.time.Duration

private val log = KotlinLogging.logger {  }

@Service
class SupportBotApiService(
    webClientConfig: WebClientConfig,
    supportBotApiConfig: SupportBotApiConfig,
    discordConfig: DiscordConfig,
    private val objectMapper: ObjectMapper
) {
    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("support-bot-service-provider")
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
        .baseUrl(supportBotApiConfig.baseUrl)
        .defaultHeader("Authorization", supportBotApiConfig.auth)
        .build()


    suspend fun forwardCreateEntitlementEvent(entitlement: Entitlement) {
        log.info { "Forwarding 'create entitlement' event to support bot" }
        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment("entitlements", "create")
                    .build()
            }
            .bodyValue(entitlement)
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
    }

    suspend fun forwardUpdateEntitlementEvent(entitlement: Entitlement) {
        log.info { "Forwarding 'update entitlement' event to support bot" }

        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment("entitlements", "update")
                    .build()
            }
            .bodyValue(entitlement)
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
    }

    suspend fun forwardDeleteEntitlementEvent(entitlement: Entitlement) {
        log.info { "Forwarding 'delete entitlement' event to support bot" }

        webClient.post()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment("entitlements", "delete")
                    .build()
            }
            .bodyValue(entitlement)
            .retrieve()
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
    }
}