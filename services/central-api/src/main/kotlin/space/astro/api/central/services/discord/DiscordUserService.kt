package space.astro.api.central.services.discord

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import space.astro.api.central.models.discord.DiscordUser
import space.astro.shared.core.config.DiscordApiConfig

/**
 * API client to fetch Discord user using access tokens
 *
 * @see fetchSelfUser
 */
@Service
class DiscordUserService(
    discordApiConfig: DiscordApiConfig,
) {

    private val client = RestClient.builder()
        .baseUrl(discordApiConfig.baseUrl)
        .build()

    /**
     * Fetches the self user related to the provided [accessToken]
     *
     * @param accessToken
     *
     * @return the Discord user data
     *
     * @throws org.springframework.web.client.HttpClientErrorException for 401 and 403 responses
     * @throws org.springframework.web.client.HttpServerErrorException
     */
    suspend fun fetchSelfUser(accessToken: String): DiscordUser =
        client.get()
            .uri("/users/@me")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body<DiscordUser>()
            ?: error("Discord returned an empty user response")
}