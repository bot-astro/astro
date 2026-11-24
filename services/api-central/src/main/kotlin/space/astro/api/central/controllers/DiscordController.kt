package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.api.central.services.DiscordUserGuildsPersistenceService
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.api.bot.response.DiscordGuildRoleBotApiResponse
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@Tag(name = "Discord")
class DiscordController(
    private val discordUserGuildsPersistenceService: DiscordUserGuildsPersistenceService,
    private val discordApiClient: DiscordApiClient,
    private val botApiClient: BotApiClient,
) {
    @Operation(
        operationId = "discord.users.getSelf",
        summary = "Get the Discord profile of the logged in user",
        description = "Returns the Discord user (id, username, avatar, email) of the logged in user, fetched from Discord.",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_SELF_USER)
    fun getSelfUser(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<DiscordUserDto> {
        val user = discordApiClient.getSelfUser(authPrincipal.userDiscordToken)
        return ResponseEntity.ok(user)
    }

    @Operation(
        operationId = "discord.guilds.list",
        summary = "Get user guilds",
        description =
            "Lists the Discord guilds the logged in user is a member of. `canManage` tells whether the user can manage the guild, " +
                "only those can be configured with the guild endpoints. Start here to find the `guildId` of a guild by its name.",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_USER_GUILDS)
    fun getUserGuilds(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<List<DiscordUserGuild>> {
        val guilds =
            discordUserGuildsPersistenceService.fetchFromDiscord(
                userId = authPrincipal.userId,
                userDiscordAccessToken = authPrincipal.userDiscordToken,
            )
        return ResponseEntity.ok(guilds)
    }

    @Operation(
        operationId = "discord.guilds.listChannels",
        summary = "Get guild channels",
        description =
            "Lists the channels of a guild (text channels, voice channels, categories...) with their type and parent category. " +
                "Use it to find the channel and category ids that other endpoints need.",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_GUILD_CHANNELS)
    fun getGuildChannels(
        @PathVariable guildId: String,
    ): ResponseEntity<List<DiscordGuildChannelBotApiResponse>> {
        val channels = botApiClient.getGuildChannels(guildId)
        return ResponseEntity.ok(channels)
    }

    @Operation(
        operationId = "discord.guilds.listRoles",
        summary = "Get guild roles",
        description = "Lists the roles of a guild. Use it to find the role ids that other endpoints need, for example for voice roles.",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_GUILD_ROLES)
    fun getGuildRoles(
        @PathVariable guildId: String,
    ): ResponseEntity<List<DiscordGuildRoleBotApiResponse>> {
        val roles = botApiClient.getGuildRoles(guildId)
        return ResponseEntity.ok(roles)
    }
}
