package space.astro.shared.core.services.bot

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import space.astro.shared.core.components.web.BotApiRoutes
import space.astro.shared.core.configs.BotApiConfig
import space.astro.shared.core.configs.WebClientConfig
import space.astro.shared.core.models.dashboard.DashboardGuildChannel
import space.astro.shared.core.models.dashboard.DashboardGuildRole
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.InterfaceData
import space.astro.shared.core.util.exceptions.BotApiPermissionException
import space.astro.shared.core.util.exceptions.NotFoundException
import java.time.Duration

@Service
class BotApiService(
    webClientConfig: WebClientConfig,
    botApiConfig: BotApiConfig,
    private val objectMapper: ObjectMapper
) {
    private final val provider: ConnectionProvider =
        ConnectionProvider.builder("bot-service-provider")
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
        .defaultHeader("Authorization", botApiConfig.auth)
        .build()

    suspend fun isBotInGuild(
        endpoint: String,
        guildID: String
    ): Boolean {
        try {
            webClient.get()
                .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.IS_BOT_IN_GUILD.replace("{guildID}", guildID)}")
                .retrieve()
                .onStatus(
                    { it == HttpStatus.NOT_FOUND },
                    { throw NotFoundException("bot not in guild $guildID")}
                )
                .onStatus(
                    { it != HttpStatus.OK },
                    { throw Throwable("${it.statusCode()} - Unexpected Response") })
                .awaitBodilessEntity()

            return true
        } catch (e: NotFoundException) {
            return false
        }
    }

    /**
     * Gets guild channels from the bot cache
     *
     * @param endpoint
     * @param guildID
     *
     * @throws [Throwable] for unknown issues
     * @return a list of [DashboardGuildChannel]
     */
    suspend fun getGuildChannels(
        endpoint: String,
        guildID: String
    ): List<DashboardGuildChannel> {
        return webClient.get()
            .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.GUILD_CHANNELS.replace("{guildID}", guildID)}")
            .retrieve()
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("data not found for guild with id $guildID")}
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
            .awaitBody()
    }

    /**
     * Gets guild roles from the bot cache
     *
     * @param endpoint
     * @param guildID
     *
     * @throws [Throwable] for unknown issues
     * @return a list of [DashboardGuildRole]
     */
    suspend fun getGuildRoles(
        endpoint: String,
        guildID: String
    ): List<DashboardGuildRole> {
        return webClient.get()
            .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.GUILD_ROLES.replace("{guildID}", guildID)}")
            .retrieve()
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("data not found for guild with id $guildID")}
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
            .awaitBody()
    }

    /**
     * Requests the bot to create a generator
     *
     * @param endpoint
     * @param guildID
     *
     * @throws [BotApiPermissionException] if bot doesn't have permissions to create the generator
     * @throws [Throwable] for unknown issues
     * @return the [GeneratorData] of the created generator if successful
     */
    suspend fun createGenerator(
        endpoint: String,
        guildID: String
    ): GeneratorData {
        return webClient.get()
            .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.CREATE_GENERATOR.replace("{guildID}", guildID)}")
            .retrieve()
            .onStatus(
                { it == HttpStatus.METHOD_NOT_ALLOWED },
                { throw BotApiPermissionException("bot doesn't have permissions to create the generator")}
            )
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("data not found for guild with id $guildID")}
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
            .awaitBody()
    }

    /**
     * Requests the bot to create an interface
     *
     * @param endpoint
     * @param guildID
     * @param channelID
     *
     * @throws [BotApiPermissionException] if bot doesn't have permissions to create the inteface
     * @throws [Throwable] for unknown issues
     * @return the [InterfaceData] of the created interface if successful
     */
    suspend fun createInterface(
        endpoint: String,
        guildID: String,
        channelID: String
    ): InterfaceData {
        return webClient.get()
            .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.CREATE_INTERFACE.replace("{guildID}", guildID).replace("{channelID}", channelID)}")
            .retrieve()
            .onStatus(
                { it == HttpStatus.METHOD_NOT_ALLOWED },
                { throw BotApiPermissionException("bot doesn't have permissions to create the generator")}
            )
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("data not found for guild with id $guildID")}
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
            .awaitBody()
    }

    /**
     * Requests the bot to update an interface
     *
     * @param endpoint
     * @param guildID
     * @param interfaceData
     *
     * @throws [BotApiPermissionException] if bot doesn't have permissions to update the interface
     * @throws [Throwable] for unknown issues
     */
    suspend fun updateInterface(
        endpoint: String,
        guildID: String,
        interfaceData: InterfaceData
    ) {
        return webClient.post()
            .uri("${endpoint.removeSuffix("/")}${BotApiRoutes.DASHBOARD.UPDATE_INTERFACE.replace("{guildID}", guildID)}")
            .bodyValue(interfaceData)
            .retrieve()
            .onStatus(
                { it == HttpStatus.METHOD_NOT_ALLOWED },
                { throw BotApiPermissionException("bot doesn't have permissions to create the generator")}
            )
            .onStatus(
                { it == HttpStatus.NOT_FOUND },
                { throw NotFoundException("data not found for guild with id $guildID")}
            )
            .onStatus(
                { it != HttpStatus.OK },
                { throw Throwable("${it.statusCode()} - Unexpected Response") })
            .awaitBody()
    }
}