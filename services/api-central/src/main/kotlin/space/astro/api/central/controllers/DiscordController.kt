package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.OpenApiConfiguration
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.api.central.services.DiscordUserGuildsPersistenceService
import space.astro.shared.core.clients.BotApiClient
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.models.api.bot.response.DiscordGuildChannelBotApiResponse
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class DiscordController(
    private val discordUserGuildsPersistenceService: DiscordUserGuildsPersistenceService,
    private val discordApiClient: DiscordApiClient,
    private val botApiClient: BotApiClient
) {

    @GetMapping(CentralApiEndpoint.DISCORD_SELF_USER)
    fun getSelfUser(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<DiscordUserDto> {
        val user = discordApiClient.getSelfUser(authPrincipal.userDiscordToken)
        return ResponseEntity.ok(user)
    }

    @GetMapping(CentralApiEndpoint.DISCORD_USER_GUILDS)
    fun getUserGuilds(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal
    ): ResponseEntity<List<DiscordUserGuild>> {
        val guilds = discordUserGuildsPersistenceService.fetchFromDiscord(
            userId = authPrincipal.userId,
            userDiscordAccessToken = authPrincipal.userDiscordToken
        )
        return ResponseEntity.ok(guilds)
    }

    @ApiResponses(
        ApiResponse(
            responseCode = "404",
            description = "NOT_FOUND: The guild was not found by the bot.",
            content = [Content(mediaType = "application/json", schema = Schema(ref = OpenApiConfiguration.ERROR_RESPONSE_SCHEMA))]
        ),
    )
    @GetMapping(CentralApiEndpoint.DISCORD_GUILD_CHANNELS)
    fun getGuildChannels(
        @PathVariable guildId: String
    ): ResponseEntity<List<DiscordGuildChannelBotApiResponse>> {
        val channels = botApiClient.getGuildChannels(guildId)
        return ResponseEntity.ok(channels)
    }
}
