package space.astro.api.central.controllers.dashboard

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.shared.core.daos.GuildDao

@RestController
class DashboardGuildUltimateController(
    private val guildDao: GuildDao,
    private val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService
) {
    @GetMapping(Mappings.Dashboard.GUILD_UPGRADE)
    suspend fun upgradeGuild(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {
        /*
         TODO:
          - Validate whether user has upgrades available
          - Perform upgrade (see deprecated premium commands)
         */
    }

    @GetMapping(Mappings.Dashboard.GUILD_DOWNGRADE)
    suspend fun downgradeGuild(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {
        /*
        TODO:
          - Validate whether user has downgrade available
          - Perform upgrade (see deprecated premium commands)
         */
    }
}