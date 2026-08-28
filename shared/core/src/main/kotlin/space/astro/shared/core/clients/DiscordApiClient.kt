package space.astro.shared.core.clients

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.discord.DiscordPartialGuildDto
import space.astro.shared.core.models.discord.DiscordTokenDto
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.properties.DiscordApiProperties
import space.astro.shared.core.properties.DiscordOAuthProperties

private val log = KotlinLogging.logger {  }

class DiscordApiClient(
    discordApiConfig: DiscordApiProperties,
) {

    private val client = RestClient.builder()
        .baseUrl(discordApiConfig.baseUrl)
        .build()

    /**
     * Exchanges a Discord OAuth flow [code] for an access token
     *
     * @param code
     * @param discordOAuthProperties required discord application information for the OAuth flow
     * @return the access token data
     * @throws AException
     */
    fun getAccessToken(
        code: String,
        discordOAuthProperties: DiscordOAuthProperties
    ): DiscordTokenDto {
        val form: MultiValueMap<String, String> = LinkedMultiValueMap()
        form.add("client_id", discordOAuthProperties.id)
        form.add("client_secret", discordOAuthProperties.secret)
        form.add("grant_type", "authorization_code")
        form.add("code", code)
        form.add("redirect_uri", discordOAuthProperties.redirectUri)
        form.add("scope", "identify email guilds")

        try {
            return client.post()
                .uri("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body<DiscordTokenDto>()
                ?: run {
                    log.error { "Discord returned an empty token response" }
                    throw AUnknownException("Failed to exchange Discord code for access token.")
                }
        } catch (e: RestClientResponseException) {
            when (e.statusCode.value()) {
                400 -> throw ABadRequestException("Received a likely malformed or expired Discord auth code", e)
                else -> throw AUnknownException("Failed to exchange Discord code for access token", e)
            }
        }
    }

    /**
     * Exchanges the [refreshToken] for a new access token
     *
     * @param refreshToken
     * @param discordOAuthProperties required discord application information for the OAuth flow
     * @return the new access token data
     * @throws AException
     */
    fun refreshToken(
        refreshToken: String,
        discordOAuthProperties: DiscordOAuthProperties
    ): DiscordTokenDto {
        val form: MultiValueMap<String, String> = LinkedMultiValueMap()
        form.add("client_id", discordOAuthProperties.id)
        form.add("client_secret", discordOAuthProperties.secret)
        form.add("grant_type", "refresh_token")
        form.add("refresh_token", refreshToken)

        try {
            return client.post()
                .uri("/oauth2/token")
                .body(form)
                .retrieve()
                .body<DiscordTokenDto>()
                ?: run {
                    log.error { "Discord returned an empty token response" }
                    throw AUnknownException("Failed to exchange Discord code for access token.")
                }
        } catch (e: RestClientResponseException) {
            throw AUnknownException("Failed to refresh Discord access token", e)
        }
    }

    /**
     * Fetches the self user related to the provided [accessToken]
     *
     * @param accessToken
     * @return the Discord user data
     * @throws AException
     */
    fun getSelfUser(accessToken: String): DiscordUserDto {
        try {
            return client.get()
                .uri("/users/@me")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body<DiscordUserDto>()
                ?: run {
                    log.error { "Discord returned an empty user response" }
                    throw AUnknownException("Failed to fetch Discord self user")
                }
        } catch (e: RestClientResponseException) {
            throw AUnknownException("Failed to fetch Discord self user", e)
        }
    }

    /**
     * Fetches the user guilds
     *
     * @param accessToken
     * @return the list of guilds of the user
     * @throws AException
     */
    fun getGuilds(accessToken: String): List<DiscordPartialGuildDto> {
        try {
            return client.get()
                .uri("/users/@me/guilds")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body<List<DiscordPartialGuildDto>>()
                ?: run {
                    log.error { "Discord returned an empty gulds response" }
                    throw AUnknownException("Failed to fetch Discord user guilds")
                }
        } catch (e: RestClientResponseException) {
            // TODO: handle auth errors properly
            throw AUnknownException("Failed to fetch Discord user guilds", e)
        }
    }
}