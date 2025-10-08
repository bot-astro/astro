package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.services.discord.DiscordUserService
import space.astro.api.central.util.getAccessToken
import space.astro.shared.core.components.web.CentralApiRoutes

@RestController
@Tag(name = "dashboard-data")
class DashboardUserController(
    val discordUserService: DiscordUserService,
) {
    @GetMapping(CentralApiRoutes.Dashboard.USERS_ME)
    suspend fun getSelfUser(exchange: ServerWebExchange): ResponseEntity<*> {
        val accessToken = exchange.getAccessToken()

        try {
            val discordUser = discordUserService.fetchSelfUser(accessToken)
            return ResponseEntity.ok(discordUser)
        } catch (e: Throwable) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }
}