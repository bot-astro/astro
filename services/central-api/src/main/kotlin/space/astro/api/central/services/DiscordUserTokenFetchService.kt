package space.astro.api.central.services

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.api.central.configs.DiscordApplicationConfig
import space.astro.api.central.models.AuthorizationWrapperDto
import space.astro.api.central.models.TokenPayloadDto
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.WebClientConfig
import java.time.Duration

private val log = KotlinLogging.logger { }

@Service
class DiscordUserTokenFetchService(
    webClientConfig: WebClientConfig,
    discordConfig: DiscordConfig,
    val discordApplicationConfig: DiscordApplicationConfig,
    val discordUserService: DiscordUserService,
    val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    val objectMapper: ObjectMapper
) {

    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("discord-user-token-fetch-service-provider")
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

    suspend fun fetchCredentialsFromCode(
        code: String
    ): AuthorizationWrapperDto {
        log.info("Generating new token")
        try {
            val tokenPayload = webClient.post()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oauth2", "token")
                        .build()
                }
                .body(
                    BodyInserters.fromFormData("client_id", discordApplicationConfig.id)
                        .with("client_secret", discordApplicationConfig.secret)
                        .with("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", discordApplicationConfig.redirectUri)
                        .with("scope", "identify email guilds")
                )
                .retrieve()
                .onStatus(
                    { it != HttpStatus.OK },
                    { throw Throwable("${it.statusCode()} - Unexpected Response") })
                .awaitBody<TokenPayloadDto>()

            val selfUser = discordUserService.fetchSelfUser(tokenPayload.accessToken)
            discordUserTokenPersistenceService.updateCredentials(selfUser.id, tokenPayload)

            return AuthorizationWrapperDto(selfUser, tokenPayload)
        } catch (t: Throwable) {
            throw RuntimeException(
                "Unable to fetch credentials from code $code!",
                t
            )
        }
    }
}