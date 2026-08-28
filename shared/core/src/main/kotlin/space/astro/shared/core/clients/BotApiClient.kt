package space.astro.shared.core.clients

import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.database.guildSettings.GeneratorData
import space.astro.shared.core.models.discord.DiscordPartialChannelDto
import space.astro.shared.core.models.discord.DiscordPartialRoleDto
import space.astro.shared.core.models.request.BotChannelCreateRequest
import space.astro.shared.core.properties.bot.BotEndpointProperties
import space.astro.shared.core.properties.bot.BotShardProperties

class BotApiClient(
    private val botEndpointProperties: BotEndpointProperties,
    private val botShardProperties: BotShardProperties
) {
    private val client = RestClient.create()

    private fun baseUrl(podId: Long): String {
        return if (botEndpointProperties.localhost)
            "http://localhost:${botEndpointProperties.port}/api/v2"
        else
            "http://${botEndpointProperties.podName}-$podId.${botEndpointProperties.serviceName}.${botEndpointProperties.namespace}:${botEndpointProperties.port}/api/v2"
    }

    private fun baseUrlForGuild(guildId: String): String {
        val shardId = calculateShardIdFromGuildId(guildId)
        val shardsPerPod = botShardProperties.totalShards.floorDiv(botShardProperties.totalPods)

        return baseUrl(shardId.floorDiv(shardsPerPod))
    }

    fun calculateShardIdFromGuildId(guildId: String): Long {
        return (guildId.toLong() shr 22) % botShardProperties.totalShards
    }

    fun isBotInGuild(
        guildId: String,
    ): Boolean {
        return try {
            client.get()
                .uri("${baseUrlForGuild(guildId)}/guilds/$guildId")
                .retrieve()

            true
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                return false

            throw AUnknownException("Failed to check if bot is in guild", e)
        }
    }

    fun getGuildChannels(
        guildId: String
    ): List<DiscordPartialChannelDto> {
        try {
            return client.get()
                .uri("${baseUrlForGuild(guildId)}/guilds/$guildId/channels")
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild channels")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to fetch guild channels", e)
        }
    }

    fun getGuildRoles(
        guildId: String
    ): List<DiscordPartialRoleDto> {
        try {
            return client.get()
                .uri("${baseUrlForGuild(guildId)}/guilds/$guildId/roles")
                .retrieve()
                .body()
                ?: throw AUnknownException("Failed to fetch guild roles")
        } catch (e: RestClientException) {
            if (e is RestClientResponseException && e.statusCode.value() == 404)
                throw ANotFoundException("Guild with id $guildId not found")

            throw AUnknownException("Failed to fetch guild roles", e)
        }
    }

    fun createChannel(
        guildId: String,
        botChannelCreateRequest: BotChannelCreateRequest
    ) {
        TODO()
        try {
            client.post()
                .uri("${baseUrlForGuild(guildId)}/guilds/$guildId/generator")
                .body(botChannelCreateRequest)
                .retrieve()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException) {
                when (e.statusCode.value()) {
                    404 -> throw ANotFoundException("Guild with id $guildId not found")
                }
            }

            throw AUnknownException("Failed to create generator", e)
        }
    }

    fun createInterface(
        guildId: String,
        channelId: String
    ) {
       TODO()
    }

    fun updateInterface(
        guildId: String,
        interfaceData: InterfaceData
    ) {
        TODO()
        try {
            client.post()
                .uri("${baseUrlForGuild(guildId)}/guilds/$guildId/interface")
                .body(interfaceData)
                .retrieve()
        } catch (e: RestClientException) {
            if (e is RestClientResponseException) {
                when (e.statusCode.value()) {
                    404 -> throw ANotFoundException("Guild with id $guildId not found")
                    405 -> throw BotApiPermissionException(
                        "Bot doesn't have permissions to update the interface"
                    )
                }
            }

            throw AUnknownException("Failed to update interface", e)
        }
    }
}