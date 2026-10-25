package space.astro.shared.core.clients

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.api.bot.request.GuildProfileUpdateBotApiRequest
import space.astro.shared.core.models.api.bot.request.InterfaceCreateBotApiRequest
import space.astro.shared.core.models.api.bot.response.DiscordGuildBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildRoleBotApiResponse
import space.astro.shared.core.models.database.guildSettings.InterfaceData
import space.astro.shared.core.properties.bot.BotEndpointProperties
import space.astro.shared.core.properties.bot.BotShardProperties
import space.astro.shared.core.utils.api.BotApiEndpoint
import tools.jackson.databind.json.JsonMapper

class BotApiClient(
    private val botEndpointProperties: BotEndpointProperties,
    private val botShardProperties: BotShardProperties,
    jsonMapper: JsonMapper,
) {
    // the injected mapper uses snake_case, same as the bot API
    private val client =
        RestClient
            .builder()
            .configureMessageConverters { converters ->
                converters.withJsonConverter(JacksonJsonHttpMessageConverter(jsonMapper))
            }.build()

    private fun baseUrl(podId: Long): String =
        if (botEndpointProperties.localhost) {
            "http://localhost:${botEndpointProperties.port}"
        } else {
            "http://${botEndpointProperties.podName}-$podId.${botEndpointProperties.serviceName}.${botEndpointProperties.namespace}:${botEndpointProperties.port}"
        }

    private fun baseUrlForGuild(guildId: String): String {
        val shardId = calculateShardIdFromGuildId(guildId)
        val shardsPerPod = botShardProperties.totalShards.floorDiv(botShardProperties.totalPods)

        return baseUrl(shardId.floorDiv(shardsPerPod))
    }

    fun calculateShardIdFromGuildId(guildId: String): Long = (guildId.toLong() shr 22) % botShardProperties.totalShards

    fun getGuild(guildId: String): DiscordGuildBotApiResponse {
        try {
            return client
                .get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to fetch guild", e)
        }
    }

    fun updateGuildProfile(
        guildId: String,
        request: GuildProfileUpdateBotApiRequest,
    ) {
        try {
            client
                .post()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_PROFILE, guildId)
                .body(request)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to update guild profile", e)
        }
    }

    fun getGuildChannels(guildId: String): List<DiscordGuildChannelBotApiResponse> {
        try {
            return client
                .get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_CHANNELS, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild channels")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to fetch guild channels", e)
        }
    }

    fun createChannel(
        guildId: String,
        request: ChannelCreateBotApiRequest,
    ): DiscordGuildChannelBotApiResponse {
        try {
            return client
                .post()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_CHANNELS, guildId)
                .body(request)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to create channel")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to create channel", e)
        }
    }

    fun deleteChannel(
        guildId: String,
        channelId: String,
    ) {
        try {
            client
                .delete()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_CHANNEL, guildId, channelId)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to delete channel", e)
        }
    }

    fun getGuildRoles(guildId: String): List<DiscordGuildRoleBotApiResponse> {
        try {
            return client
                .get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_ROLES, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild roles")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to fetch guild roles", e)
        }
    }

    fun createInterface(
        guildId: String,
        request: InterfaceCreateBotApiRequest,
    ): InterfaceData {
        try {
            return client
                .post()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_INTERFACE, guildId)
                .body(request)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to create interface")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild or channel not found for guild with id $guildId")
            }

            throw AUnknownException("Failed to create interface", e)
        }
    }

    fun updateInterface(
        guildId: String,
        interfaceData: InterfaceData,
    ) {
        try {
            client
                .put()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_INTERFACE, guildId)
                .body(interfaceData)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild, channel or message not found for guild with id $guildId")
            }

            throw AUnknownException("Failed to update interface", e)
        }
    }

    fun deleteInterface(
        guildId: String,
        channelId: String,
        messageId: String,
    ) {
        try {
            client
                .delete()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_INTERFACE_MESSAGE, guildId, channelId, messageId)
                .retrieve()
                .toBodilessEntity()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404) {
                throw ANotFoundException("Guild with id $guildId not found")
            }

            throw AUnknownException("Failed to delete interface", e)
        }
    }
}
