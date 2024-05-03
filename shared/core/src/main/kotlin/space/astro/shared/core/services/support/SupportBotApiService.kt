package space.astro.shared.core.services.support

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.shared.core.configs.SupportBotApiConfig
import space.astro.shared.core.configs.WebClientConfig
import space.astro.shared.core.util.exceptions.NotFoundException
import space.astro.shared.core.util.exceptions.UnknownException
import java.time.Duration

private val log = KotlinLogging.logger {  }

/**
 * Api client to interact with the support bot service
 *
 * @see addPremiumRoleToUser
 * @see removePremiumRoleFromUser
 */
@Service
class SupportBotApiService(
    webClientConfig: WebClientConfig,
    supportBotApiConfig: SupportBotApiConfig,
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


    /**
     * Requests to add the premium role in the Astro support server to the user with the given [userID]
     *
     * @param userID the id of the user that should receive the premium role
     *
     * @throws [NotFoundException] if the user wasn't found in the support server
     * @throws [UnknownException] for all other possible errors
     */
    suspend fun addPremiumRoleToUser(userID: String) {
        log.info { "Requesting premium role add to support-bot service" }

        webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment("premium", "role", "add", userID)
                    .build()
            }
            .retrieve()
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("${it.statusCode()} - User with the provided ID wasn't found in the support server") })
            .onStatus(
                { it != HttpStatus.OK },
                { throw UnknownException("${it.statusCode()} - Unexpected Response") })
    }

    /**
     * Requests to remove the premium role in the Astro support server from the user with the given [userID]
     *
     * @param userID the id of the user that should get the premium role removed
     *
     * @throws [NotFoundException] if the user wasn't found in the support server
     * @throws [UnknownException] for all other possible errors
     */
    suspend fun removePremiumRoleFromUser(userID: String) {
        log.info { "Requesting premium role removal to support-bot service" }

        webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .pathSegment("premium", "role", "remove", userID)
                    .build()
            }
            .retrieve()
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("${it.statusCode()} - User with the provided ID wasn't found in the support server") })
            .onStatus(
                { it != HttpStatus.OK },
                { throw UnknownException("${it.statusCode()} - Unexpected Response") })
    }
}