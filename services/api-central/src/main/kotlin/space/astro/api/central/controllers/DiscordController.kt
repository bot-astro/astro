package space.astro.api.central.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.api.central.services.DiscordUserGuildsPersistenceService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.ANotFoundException
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.models.discord.DiscordUserDto
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
class DiscordController(
    private val discordUserGuildsPersistenceService: DiscordUserGuildsPersistenceService,
    private val discordApiClient: DiscordApiClient
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

    @GetMapping(CentralApiEndpoint.DISCORD_GUILD_CHANNELS)
    fun getGuildChannels(
        @PathVariable guildId: String,
        @AuthenticationPrincipal authPrincipal: AuthPrincipal
    ) {
        val guild = discordUserGuildsPersistenceService.getUserGuild(authPrincipal.userId, guildId)
            ?: run {
                val guilds = discordUserGuildsPersistenceService.fetchFromDiscord(
                    userId = authPrincipal.userId,
                    userDiscordAccessToken = authPrincipal.userDiscordToken
                )
                guilds.find { it.id == guildId }
                    ?: throw ANotFoundException("Guild with id $guildId not found")
            }

        if (!guild.canManage)
            throw AUnauthorizedException("You don't have permissions to manage this guild")


    }
}