package space.astro.api.central.controllers.dashboard

import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.getAccessToken
import space.astro.api.central.configs.getUserID
import space.astro.api.central.models.dashboard.DashboardGuildChannel
import space.astro.api.central.models.dashboard.DashboardGuildDto
import space.astro.api.central.models.dashboard.DashboardGuildRole
import space.astro.api.central.services.DiscordGuildsFetchService
import space.astro.shared.core.daos.GuildDao

private val log = KotlinLogging.logger {  }

@RestController
class DashboardGuildsController(
    private val discordGuildsFetchService: DiscordGuildsFetchService,
    private val guildDao: GuildDao
) {
    @GetMapping(Mappings.Dashboard.guilds)
    suspend fun getUserGuilds(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val accessToken = exchange.getAccessToken()
        val guilds = discordGuildsFetchService.fetchGuilds(accessToken)

        return ResponseEntity.ok(guilds.map { partialGuild ->
            DashboardGuildDto(
                id = partialGuild.id,
                name = partialGuild.name,
                icon = partialGuild.icon,
                canManage = Permission.getPermissions(partialGuild.permissions).any {
                    it == Permission.MANAGE_CHANNEL || it == Permission.MANAGE_SERVER || it == Permission.ADMINISTRATOR
                },
                settings = guildDao.get(partialGuild.id)
            )
        })
    }

    @GetMapping(Mappings.Dashboard.guildChannels)
    suspend fun getGuildChannels(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val accessToken = exchange.getAccessToken()
        val channels = discordGuildsFetchService.fetchGuildChannels(accessToken = accessToken, guildID = guildID)

        return ResponseEntity.ok(channels.map { partialChannel ->
            DashboardGuildChannel(
                id = partialChannel.id,
                name = partialChannel.name,
                type = partialChannel.type,
                parentID = partialChannel.parentID,
                parentName = channels.firstOrNull { it.id == partialChannel.parentID }?.name
            )
        })
    }

    @GetMapping(Mappings.Dashboard.guildRoles)
    suspend fun getGuildRoles(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val accessToken = exchange.getAccessToken()
        val roles = discordGuildsFetchService.fetchGuildRoles(accessToken = accessToken, guildID = guildID)

        return ResponseEntity.ok(roles.map { partialRole ->
            DashboardGuildRole(
                id = partialRole.id,
                name = partialRole.name,
                color = partialRole.color,
                position = partialRole.position
            )
        })
    }
}