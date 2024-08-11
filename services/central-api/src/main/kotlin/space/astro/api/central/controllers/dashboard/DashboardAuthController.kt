package space.astro.api.central.controllers.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import space.astro.api.central.configs.CentralApiConfig
import space.astro.shared.core.components.web.CentralApiRoutes
import space.astro.api.central.util.getUserID
import space.astro.api.central.models.discord.OAuth2AuthorizationResponseDto
import space.astro.api.central.models.discord.OAuth2GuildInfo
import space.astro.api.central.services.dashboard.DashboardGuildsPersistenceService
import space.astro.api.central.services.discord.DiscordUserTokenFetchService
import space.astro.api.central.services.discord.DiscordUserTokenPersistenceService
import space.astro.api.central.services.dashboard.WebSessionService
import space.astro.shared.core.util.exceptions.BadRequestException
import space.astro.shared.core.util.exceptions.UnauthorizedException

private val log = KotlinLogging.logger { }

@RestController
@Tag(name = "dashboard-auth")
class DashboardAuthController(
    val discordUserTokenFetchService: DiscordUserTokenFetchService,
    val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    val webSessionService: WebSessionService,
    val dashboardGuildsPersistenceService: DashboardGuildsPersistenceService,
    val centralApiConfig: CentralApiConfig
) {

    @GetMapping(CentralApiRoutes.Dashboard.LOGIN)
    suspend fun authorizeDiscord(
        exchange: ServerWebExchange,
        @PathVariable code: String
    ): ResponseEntity<*> {
        log.info { "Authorizing discord user with code $code" }

        val userAndToken = try {
            discordUserTokenFetchService.exchangeCodeForAccessTokenAndFetchSelfUser(code)
        } catch (e: BadRequestException) {
            return ResponseEntity.badRequest().build<Any>()
        } catch (e: UnauthorizedException) {
            return ResponseEntity.badRequest().build<Any>()
        }

        val user = userAndToken.user
        val guild = userAndToken.token.guild?.let {
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

        val cookie = ResponseCookie.from(centralApiConfig.sessionCookieName, sessionToken)
            .path("/")
            .maxAge(centralApiConfig.sessionCookieMaxAgeInSeconds)
            .httpOnly(centralApiConfig.sessionCookieHttpOnly)
            .secure(centralApiConfig.sessionCookieSecure)
            .sameSite(centralApiConfig.sessionCookieSameSite)
            .domain(centralApiConfig.sessionCookieDomain)
            .build()

        val headers = HttpHeaders().apply {
            set(HttpHeaders.SET_COOKIE, cookie.toString())
        }

        log.info { "Successfully authorized discord user with id ${user.id} - response $response" }

        return ResponseEntity.ok()
            .headers(headers)
            .body(response)
    }

    @GetMapping(CentralApiRoutes.Dashboard.LOGOUT)
    suspend fun logoutDiscord(
        exchange: ServerWebExchange
    ): ResponseEntity<*> {
        val userID = exchange.getUserID()

        discordUserTokenPersistenceService.deleteToken(userID)
        dashboardGuildsPersistenceService.deleteUserGuilds(userID)
        webSessionService.deleteSessions(userID)

        return ResponseEntity.ok().build<Any>()
    }
}