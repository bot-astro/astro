package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.astro.api.central.components.OpenApiConfiguration
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.responses.LoginResponse
import space.astro.api.central.services.AuthSessionService
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.utils.api.CentralApiEndpoint

@RestController
@RequestMapping
@Tag(name = "Authentication")
class AuthController(
    private val discordApiClient: DiscordApiClient,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val authSessionService: AuthSessionService,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService
) {

    @Operation(
        summary = "Login via Discord",
        description = "Login via Discord and get a session cookie",
        operationId = "loginViaDiscord"
    )
    @PostMapping(CentralApiEndpoint.LOGIN_VIA_DISCORD)
    fun loginViaDiscord(@RequestParam code: String): ResponseEntity<LoginResponse> {
        val discordToken = discordApiClient.getAccessToken(code, discordOAuthProperties)
        val discordUser = discordApiClient.getSelfUser(discordToken.accessToken)

        discordUserTokenPersistenceService.upsert(discordUser.id, discordToken)

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


    @Operation(
        summary = "Logout",
        description = "Logout and delete the session cookie",
        operationId = "logout",
        security = [SecurityRequirement(name = OpenApiConfiguration.SESSION_SECURITY_NAME)]
    )
    @PostMapping(CentralApiEndpoint.LOGOUT)
    fun logout(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal
    ): ResponseEntity<Void> {
        discordUserTokenPersistenceService.delete(authPrincipal.userId)
        authSessionService.deleteSession(authPrincipal.userId, authPrincipal.sessionId)
        return ResponseEntity.noContent().build()
    }
}