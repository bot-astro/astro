package space.astro.shared.core.services.discord

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import space.astro.shared.core.configs.DiscordConfig
import space.astro.shared.core.models.discord.DiscordEntitlementData
import java.net.URI
import java.util.*

private val log = KotlinLogging.logger { }

@Service
class DiscordEntitlementsFetchService(
    private val discordConfig: DiscordConfig,
) {
    private val httpClient = HttpClient(Apache) {
        expectSuccess = true
        install(Logging)
        install(ContentNegotiation) {
            jackson()
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }


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
    ): List<DiscordEntitlementData> {
        log.info("Fetching entitlements")

        try {
            val uri = UriComponentsBuilder.fromUri(URI(discordConfig.baseUrl))
                .pathSegment("applications", applicationId, "entitlements")
                .queryParamIfPresent("guild_id", guildId?.let { Optional.of(it) } ?: Optional.empty<String>())
                .queryParamIfPresent("user_id", userId?.let { Optional.of(it) } ?: Optional.empty<String>())
                .build()
                .toUriString()

            return httpClient.get(uri) {
                header("Authorization", "Bot $authToken")
            }.body()
        } catch (t: Throwable) {
            throw RuntimeException(
                "Unable to fetch entitlements for bot with id $applicationId!",
                t
            )
        }
    }
}