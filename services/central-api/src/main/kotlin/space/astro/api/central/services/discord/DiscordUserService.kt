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
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.configs.WebClientConfig
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.util.exceptions.UnauthorizedException
import space.astro.shared.core.util.exceptions.UnknownException
import java.time.Duration

/**
 * API client to fetch Discord user using access tokens
 *
 * @see fetchSelfUser
 */
@Service
class DiscordUserService(
    webClientConfig: WebClientConfig,
    discordConfig: DiscordConfig,
    val objectMapper: ObjectMapper
) {

    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("discord-user-service-provider")
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
     * Fetches the self user related to the provided [accessToken]
     *
     * @param accessToken
     *
     * @return the Discord user data
     *
     * @throws UnauthorizedException
     * @throws UnknownException
     */
    suspend fun fetchSelfUser(accessToken: String): DiscordUserDto {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.pathSegment("users", "@me")
                    .build()
            }
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .onStatus(
                { it == HttpStatus.UNAUTHORIZED || it == HttpStatus.FORBIDDEN },
                { throw UnauthorizedException("Don't have permissions to fetch self user with the provided token - status: ${it.statusCode()}") }
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw UnknownException("Failed to fetch user - status: ${it.statusCode()}") }
            )
            .awaitBody()
    }
}