package space.astro.api.central.controllers.dashboard

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.getAccessToken
import space.astro.api.central.services.DiscordUserService

private val log = KotlinLogging.logger { }

@RestController
class DashboardUserController(
    val discordUserService: DiscordUserService,
) {
    @GetMapping(Mappings.Dashboard.usersMe)
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