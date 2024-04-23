package space.astro.api.central.controllers.dashboard

import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.Mappings
import space.astro.api.central.configs.getUserID
import space.astro.api.central.models.discord.OAuth2AuthorizationResponseDto
import space.astro.api.central.models.discord.OAuth2GuildInfo
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.api.central.services.discord.DiscordUserTokenFetchService
import space.astro.api.central.services.discord.DiscordUserTokenPersistenceService
import space.astro.api.central.services.dashboard.WebSessionService

private val log = KotlinLogging.logger { }

@RestController
class DashboardAuthController(
    val discordUserTokenFetchService: DiscordUserTokenFetchService,
    val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    val webSessionService: WebSessionService,
    val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService
) {

    @GetMapping(Mappings.Dashboard.LOGIN)
    suspend fun authorizeDiscord(@PathVariable code: String): ResponseEntity<*> {
        log.info { "Authorizing discord user with code $code" }

        // fetch credentials + user from discord
        val authorizationWrapper = discordUserTokenFetchService.fetchCredentialsFromCode(code)

        val user = authorizationWrapper.user
        val guild = authorizationWrapper.token.guild?.let {
            OAuth2GuildInfo(
                id = it.id,
                name = it.name,
                icon = it.icon
            )
        }

        val sessionToken = webSessionService.createSession(user.id)

        val response = OAuth2AuthorizationResponseDto(
            sessionToken,
            user,
            guild
        )

        log.info { "Successfully authorized discord user with id ${user.id} - response $response" }
        return ResponseEntity.ok(response)
    }

    @GetMapping(Mappings.Dashboard.LOGOUT)
    suspend fun logoutDiscord(
        @RequestHeader("Authorization") sessionToken: String,
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()
        discordUserTokenPersistenceService.deleteCredentials(userID)
        dashboardGuildsPersistenceService.deleteUserGuilds(userID)
        webSessionService.deleteSessions(userID)
        return ResponseEntity.ok(null)
    }
}