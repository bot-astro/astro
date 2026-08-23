package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.shared.core.daos.ConfigurationErrorDao

@RestController
@Tag(name = "dashboard-data")
class DashboardGuildErrorController(
    private val configurationErrorDao: ConfigurationErrorDao
) {
    @GetMapping(CentralApiRoutes.Dashboard.GUILD_ERRORS)
    suspend fun getGuildErrors(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        val errors = configurationErrorDao.getOfLastSevenDays(guildID)
        return ResponseEntity.ok(errors)
    }

    @DeleteMapping(CentralApiRoutes.Dashboard.GUILD_ERRORS)
    suspend fun clearGuildErrors(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) : ResponseEntity<*> {
        configurationErrorDao.clear(guildID)
        return ResponseEntity.ok().build<Any>()
    }
}