package space.astro.bot.api.controllers

import dev.minn.jda.ktx.coroutines.await
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnknownException
import space.astro.shared.core.models.api.bot.request.ChannelCreateBotApiRequest
import space.astro.shared.core.models.api.bot.response.DiscordGuildBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildRoleBotApiResponse
import space.astro.shared.core.utils.api.BotApiEndpoint

@RestController
@RequestMapping
@Tag(name = "Discord Guild")
class DiscordGuildController(
    private val shardManager: ShardManager
) {

    @Operation(
        summary = "Get guild",
        description = "Returns the guild if the bot is in it, 404 otherwise",
        operationId = "getGuild"
    )
    @GetMapping(BotApiEndpoint.DISCORD_GUILD)
    suspend fun getGuild(
        @PathVariable guildId: String
    ): ResponseEntity<DiscordGuildBotApiResponse> {
        val guild = shardManager.getGuildById(guildId)
            ?: throw ANotFoundException("Guild not found")

        val response = DiscordGuildBotApiResponse(
            id = guild.id,
            name = guild.name,
            icon = guild.iconUrl
        )

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Get guild channels",
        description = "Returns a list of channels for the specified guild",
        operationId = "getGuildChannels"
    )
    @GetMapping(BotApiEndpoint.DISCORD_GUILD_CHANNELS)
    suspend fun getGuildChannels(
        @PathVariable guildId: String
    ): ResponseEntity<List<DiscordGuildChannelBotApiResponse>> {
        val guild = shardManager.getGuildById(guildId)
            ?: throw ANotFoundException("Guild not found")

        val channels = guild.channels.map { channel ->
            val parent = if (channel is ICategorizableChannel) {
                channel.parentCategory
            } else {
                null
            }

            DiscordGuildChannelBotApiResponse(
                id = channel.id,
                name = channel.name,
                type = channel.type.id,
                parentId = parent?.id,
                parentName = parent?.name
            )
        }

        return ResponseEntity.ok(channels)
    }

    @Operation(
        summary = "Create channel",
        description = "Creates a channel in the specified guild",
        operationId = "createChannel"
    )
    @PostMapping(BotApiEndpoint.DISCORD_GUILD_CHANNELS)
    suspend fun createChannel(
        @PathVariable guildId: String,
        @RequestBody request: ChannelCreateBotApiRequest
    ): ResponseEntity<DiscordGuildChannelBotApiResponse> {
        val guild = shardManager.getGuildById(guildId)
            ?: throw ANotFoundException("Guild not found")

        val category = request.categoryId?.let {
            guild.getCategoryById(it) ?: throw ANotFoundException("Category with id $it not found")
        }

        try {
            val channel = when (request.channelType) {
                ChannelCreateBotApiRequest.ChannelCreateType.TEXT ->
                    guild.createTextChannel(request.name).apply { category?.let { setParent(it) } }.await()
                ChannelCreateBotApiRequest.ChannelCreateType.VOICE ->
                    guild.createVoiceChannel(request.name).apply { category?.let { setParent(it) } }.await()
                ChannelCreateBotApiRequest.ChannelCreateType.CATEGORY ->
                    guild.createCategory(request.name).await()
            }

            val response = DiscordGuildChannelBotApiResponse(
                id = channel.id,
                name = channel.name,
                type = channel.type.id,
                parentId = category?.id,
                parentName = category?.name
            )

            return ResponseEntity.ok(response)
        } catch (e: ErrorResponseException) {
            throw ABadRequestException(e.message ?: "Error from Discord API", e)
        } catch (e: InsufficientPermissionException) {
            throw ABadRequestException("Insufficient permissions to create channel", e)
        } catch (e: Exception) {
            throw AUnknownException("An unexpected error occurred", e)
        }
    }

    @Operation(
        summary = "Get guild roles",
        description = "Returns a list of roles for the specified guild",
        operationId = "getGuildRoles"
    )
    @GetMapping(BotApiEndpoint.DISCORD_GUILD_ROLES)
    suspend fun getGuildRoles(
        @PathVariable guildId: String
    ): ResponseEntity<List<DiscordGuildRoleBotApiResponse>> {
        val guild = shardManager.getGuildById(guildId)
            ?: throw ANotFoundException("Guild not found")

        val roles = guild.roles.map { role ->
            DiscordGuildRoleBotApiResponse(
                id = role.id,
                name = role.name,
                color = role.colorRaw,
                position = role.position
            )
        }

        return ResponseEntity.ok(roles)
    }
}