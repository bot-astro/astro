package space.astro.api.central.services.discord

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
import space.astro.api.central.models.AuthUserAndAccessTokenWrapperDto
import space.astro.api.central.models.discord.TokenPayloadWithOptionalGuildDto
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.WebClientConfig
import space.astro.shared.core.util.exceptions.BadRequestException
import space.astro.shared.core.util.exceptions.UnauthorizedException
import space.astro.shared.core.util.exceptions.UnknownException
import java.time.Duration

private val log = KotlinLogging.logger {  }

/**
 * API client to exchange discord oauth code for access tokens and to exchange refresh tokens for new access tokens
 *
 * @see exchangeCodeForAccessTokenAndFetchSelfUser
 * @see refreshToken
 */
@Service
class DiscordUserTokenFetchService(
    webClientConfig: WebClientConfig,
    discordConfig: DiscordConfig,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val discordUserService: DiscordUserService,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val objectMapper: ObjectMapper
) {

    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("discord-user-token-fetch-service-provider")
            .maxIdleTime(Duration.ofSeconds(webClientConfig.httpMaxIdleTime))
            .maxLifeTime(Duration.ofSeconds(webClientConfig.httpMaxLifeTime))
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
     * Exchanges the code for an access token (and relative data).
     * After getting the access token this also fetches the user for that token
     *
     * @param code
     *
     * @throws BadRequestException if the code is not valid
     * @throws UnauthorizedException this should not happen usually and it's logged internally when it happens
     * @throws UnknownException for other issues
     *
     * @return the user and the access token in [AuthUserAndAccessTokenWrapperDto]
     */
    suspend fun exchangeCodeForAccessTokenAndFetchSelfUser(
        code: String
    ): AuthUserAndAccessTokenWrapperDto {
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
                { it == HttpStatus.BAD_REQUEST },
                { throw BadRequestException("${it.statusCode()} - Probably a malformed or expired token from the frontend dashboard") })
            .onStatus(
                { it != HttpStatus.OK },
                { throw UnknownException("${it.statusCode()} - Unable to fetch credentials from code $code") })
            .awaitBody<TokenPayloadWithOptionalGuildDto>()

        log.info("Exchanged discord code for access token")

        try {
            val selfUser = discordUserService.fetchSelfUser(tokenPayload.accessToken)
            discordUserTokenPersistenceService.updateToken(selfUser.id, tokenPayload.asTokenPayloadDto())

            log.info("Fetched discord self user")

            return AuthUserAndAccessTokenWrapperDto(selfUser, tokenPayload)
        } catch (e: UnauthorizedException) {
            log.error { "Received unauthorized exception when trying to fetch the self user, check the oauth scopes and make sure they include 'identify'" }
            throw e
        }
    }

    /**
     * Exchanges the [refreshToken] for a new [AuthUserAndAccessTokenWrapperDto]
     *
     * @param refreshToken
     */
    suspend fun refreshToken(
        refreshToken: String
    ): AuthUserAndAccessTokenWrapperDto {
        try {
            val tokenPayload = webClient.post()
                .uri { uriBuilder ->
                    uriBuilder.pathSegment("oauth2", "token")
                        .build()
                }
                .body(
                    BodyInserters.fromFormData("client_id", discordApplicationConfig.id)
                        .with("client_secret", discordApplicationConfig.secret)
                        .with("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken)
                )
                .retrieve()
                .onStatus(
                    { it != HttpStatus.OK },
                    { throw UnknownException("${it.statusCode()} - Unexpected Response") })
                .awaitBody<TokenPayloadWithOptionalGuildDto>()

            val selfUser = discordUserService.fetchSelfUser(tokenPayload.accessToken)
            discordUserTokenPersistenceService.updateToken(selfUser.id, tokenPayload.asTokenPayloadDto())

            return AuthUserAndAccessTokenWrapperDto(selfUser, tokenPayload)
        } catch (t: Throwable) {
            throw RuntimeException(
                "Unable to refresh access token from refresh token!",
                t
            )
        }
    }
}