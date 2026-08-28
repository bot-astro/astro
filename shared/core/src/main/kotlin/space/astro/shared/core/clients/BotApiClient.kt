package space.astro.shared.core.clients

import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.api.bot.response.DiscordGuildBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildRoleBotApiResponse
import space.astro.shared.core.properties.bot.BotEndpointProperties
import space.astro.shared.core.properties.bot.BotShardProperties
import space.astro.shared.core.utils.api.BotApiEndpoint

class BotApiClient(
    private val botEndpointProperties: BotEndpointProperties,
    private val botShardProperties: BotShardProperties
) {
    private val client = RestClient.create()

    private fun baseUrl(podId: Long): String {
        return if (botEndpointProperties.localhost)
            "http://localhost:${botEndpointProperties.port}"
        else
            "http://${botEndpointProperties.podName}-$podId.${botEndpointProperties.serviceName}.${botEndpointProperties.namespace}:${botEndpointProperties.port}"
    }

    private fun baseUrlForGuild(guildId: String): String {
        val shardId = calculateShardIdFromGuildId(guildId)
        val shardsPerPod = botShardProperties.totalShards.floorDiv(botShardProperties.totalPods)

        return baseUrl(shardId.floorDiv(shardsPerPod))
    }

    fun calculateShardIdFromGuildId(guildId: String): Long {
        return (guildId.toLong() shr 22) % botShardProperties.totalShards
    }

    fun getGuild(
        guildId: String
    ): DiscordGuildBotApiResponse {
        try {
            return client.get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to fetch guild", e)
        }
    }

    fun getGuildChannels(
        guildId: String
    ): List<DiscordGuildChannelBotApiResponse> {
        try {
            return client.get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_CHANNELS, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild channels")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to fetch guild channels", e)
        }
    }

    fun createChannel(
        guildId: String,
        request: ChannelCreateBotApiRequest
    ): DiscordGuildChannelBotApiResponse {
        try {
            return client.post()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_CHANNELS, guildId)
                .body(request)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to create channel")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to create channel", e)
        }
    }

    fun getGuildRoles(
        guildId: String
    ): List<DiscordGuildRoleBotApiResponse> {
        try {
            return client.get()
                .uri(baseUrlForGuild(guildId) + BotApiEndpoint.DISCORD_GUILD_ROLES, guildId)
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild roles")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to fetch guild roles", e)
        }
    }
}