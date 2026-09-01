package space.astro.api.central.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import space.astro.api.central.components.OpenApiConfiguration
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.AuthSessionService
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.OAuthStateService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.models.database.UserEntity
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.properties.FrontendProperties
import space.astro.shared.core.repositories.UserRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.net.URI

@RestController
@RequestMapping
@Tag(name = "Authentication")
class AuthController(
    private val discordApiClient: DiscordApiClient,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val authSessionService: AuthSessionService,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val oAuthStateService: OAuthStateService,
    private val frontendProperties: FrontendProperties,
    private val userRepository: UserRepository
) {

    @Operation(
        summary = "Login via Discord",
        description = "Perform Discord OAuth flow, " +
                "will result in a session cookie on a successful flow " +
                "and instead on a query parameter called `error_code` set in the baseUrl of the frontend on errors",
    )
    @GetMapping(CentralApiEndpoint.DISCORD_LOGIN)
    fun discord(
        @RequestParam("redirectPath") redirectPath: String,
        @RequestParam("invite", defaultValue = "false") invite: Boolean,
        @RequestParam("invite_guild_id", required = false) inviteGuildId: String? = null,
    ): ResponseEntity<Void> {
        val state = oAuthStateService.create(redirectPath)

        val scopes = if (invite || inviteGuildId != null) {
            "identify email guilds bot applications.commands"
        } else {
            "identify email guilds"
        }

        val uri = UriComponentsBuilder
            .fromUriString("https://discord.com/oauth2/authorize")
            .queryParam("client_id", discordOAuthProperties.id)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", discordOAuthProperties.redirectUri)
            .queryParam("scope", scopes)
            .queryParam("state", state)
            .queryParam("prompt", "none")
            .also {
                if (invite || inviteGuildId != null) {
                    it.queryParam("permissions", "8")
                }
                if (inviteGuildId != null) {
                    it.queryParam("disable_guild_select", "true")
                    it.queryParam("guild_id", inviteGuildId)
                }
            }
            .build()
            .encode()
            .toUri()

        return ResponseEntity
            .status(HttpStatus.TEMPORARY_REDIRECT)
            .location(uri)
            .build()
    }

    @Operation(
        summary = "Discord OAuth callback",
        description = "Endpoint called by Discord containing the code & state for the first part of the OAuth flow",
        hidden = true
    )
    @GetMapping(CentralApiEndpoint.DISCORD_OAUTH_CALLBACK)
    fun discordCallback(
        @RequestParam("code", required = false) code: String? = null,
        @RequestParam("state", required = false) state: String? = null,
        @RequestParam("error", required = false) error: String? = null,
    ): ResponseEntity<Void> {
        if (error != null || code == null || state == null) {
            return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI("${frontendProperties.baseUrl}?error_code=discord_oauth_error"))
                .build()
        }

        val redirectPath = oAuthStateService.consume(state)
            ?: return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI("${frontendProperties.baseUrl}?error_code=invalid_oauth_state"))
                .build()

        val discordToken = discordApiClient.getAccessToken(code, discordOAuthProperties)
        val discordUser = discordApiClient.getSelfUser(discordToken.accessToken)

        discordUserTokenPersistenceService.upsert(discordUser.id, discordToken)

        val authSession = authSessionService.createSession(discordUser.id)

        val cookie = authSessionService.createSessionCookie(authSession)

        val headers = HttpHeaders().apply {
            set(HttpHeaders.SET_COOKIE, cookie.toString())
        }

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI("${frontendProperties.baseUrl}${redirectPath}"))
            .headers(headers)
            .build()
    }

    @Operation(
        summary = "Get logged in user",
        description = "Returns the logged in user entity (from Astro database, not Discord)",
        security = [SecurityRequirement(name = OpenApiConfiguration.SESSION_SECURITY_NAME)]
    )
    @GetMapping(CentralApiEndpoint.ME)
    fun me(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal
    ): ResponseEntity<UserEntity> {
        val user = userRepository.findByUserID(authPrincipal.userId)
            ?: run {
                val userEntity = UserEntity(userID = authPrincipal.userId)
                userRepository.save(userEntity)
                userEntity
            }

        return ResponseEntity.ok(user)
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

        return ResponseEntity.noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                authSessionService.createExpiredSessionCookie().toString()
            )
            .build()
    }
}