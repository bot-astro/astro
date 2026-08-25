package space.astro.api.central.controllers.v2

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.models.auth.AuthSession
import space.astro.api.central.models.responses.LoginResponse
import space.astro.api.central.services.dashboard.AuthSessionService
import space.astro.api.central.services.discord.DiscordUserTokenPersistenceService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.properties.DiscordApplicationProperties

@RestController
@RequestMapping("/v2/auth")
class AuthController(
    private val discordApiClient: DiscordApiClient,
    private val discordApplicationProperties: DiscordApplicationProperties,
    private val authSessionService: AuthSessionService,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService
) {

    @GetMapping("/login")
    fun loginViaDiscord(@RequestParam code: String): ResponseEntity<LoginResponse> {
        // TODO: Store the token via DiscordUserTokenPersistanceService?
        val discordToken = discordApiClient.getAccessToken(code, discordApplicationProperties)
        val discordUser = discordApiClient.getSelfUser(discordToken.accessToken)

        val authSession = authSessionService.createSession(discordUser.id)

        val response = LoginResponse(
            discordUser = discordUser,
            discordGuild = discordToken.guild
        )

        val cookie = authSessionService.createSessionCookie(authSession)

        val headers = HttpHeaders().apply {
            set(HttpHeaders.SET_COOKIE, cookie.toString())
        }

        return ResponseEntity.ok()
            .headers(headers)
            .body(response)
    }


    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal authSession: AuthSession
    ): ResponseEntity<Void> {
        // TODO: delete persisted token
        authSessionService.deleteSession(authSession.userId, authSession.sessionId)
        return ResponseEntity.noContent().build()
    }
}