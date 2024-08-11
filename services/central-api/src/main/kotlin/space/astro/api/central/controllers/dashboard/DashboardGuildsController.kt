package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.api.central.util.getAccessToken
import space.astro.api.central.util.getUserID
import space.astro.api.central.models.dashboard.DashboardGuildDto
import space.astro.api.central.services.bot.PodMetaCalculatorService
import space.astro.api.central.services.discord.DiscordGuildsFetchService
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.shared.core.services.bot.BotApiService
import space.astro.shared.core.util.exceptions.NotFoundException

private val log = KotlinLogging.logger {  }

@RestController
@Tag(name = "dashboard-discord-data")
class DashboardGuildsController(
    private val discordGuildsFetchService: DiscordGuildsFetchService,
    private val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService,
    private val podMetaCalculatorService: PodMetaCalculatorService,
    private val botApiService: BotApiService
) {
    @GetMapping(CentralApiRoutes.Dashboard.GUILDS)
    suspend fun getUserGuilds(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val accessToken = exchange.getAccessToken()
        val userID = exchange.getUserID()

        val dashboardGuilds = discordGuildsFetchService.fetchGuilds(accessToken)
            .map { partialGuild ->
                DashboardGuildDto(
                    id = partialGuild.id,
                    name = partialGuild.name,
                    icon = partialGuild.icon,
                    canManage = Permission.getPermissions(partialGuild.permissions).any {
                        it == Permission.MANAGE_CHANNEL || it == Permission.MANAGE_SERVER || it == Permission.ADMINISTRATOR
                    },
                )
            }

        dashboardGuildsPersistenceService.updateUserGuilds(userID, dashboardGuilds)

        return ResponseEntity.ok(dashboardGuilds)
    }

    @GetMapping(CentralApiRoutes.Dashboard.GUILD_CHANNELS)
    suspend fun getGuildChannels(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)

        try {
            val channels = botApiService.getGuildChannels(endpoint, guildID)
            return ResponseEntity.ok(channels)
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }

    @GetMapping(CentralApiRoutes.Dashboard.GUILD_ROLES)
    suspend fun getGuildRoles(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val endpoint = podMetaCalculatorService.calculatePodEndpoint(guildID)

        try {
            val roles = botApiService.getGuildRoles(endpoint, guildID)
            return ResponseEntity.ok(roles)
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build<Any>()
        }
    }
}