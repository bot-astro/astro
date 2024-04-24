package space.astro.api.central.controllers.dashboard

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings

@RestController
class DashboardGuildErrorController(

) {
    @GetMapping(Mappings.Dashboard.GUILD_ERRORS)
    suspend fun getGuildErrors(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {
        /*
         TODO:
          - query errors
          - respond
         */
    }

    @DeleteMapping(Mappings.Dashboard.GUILD_ERRORS)
    suspend fun clearGuildErrors(
        @PathVariable guildID: String,
        exchange: ServerWebExchange
    ) {
        /*
         TODO:
          - clear
          - respond with ok
         */
    }
}