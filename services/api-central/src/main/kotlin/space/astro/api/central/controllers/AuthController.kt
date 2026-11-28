package space.astro.api.central.controllers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.responses.AuthUserResponse
import space.astro.api.central.services.AuthSessionService
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.McpOAuthService
import space.astro.api.central.services.OAuthStateService
import space.astro.shared.core.clients.BigQueryService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.models.analytics.InstallEventData
import space.astro.shared.core.models.database.UserEntity
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.properties.FrontendProperties
import space.astro.shared.core.repositories.mongo.UserRepository
import space.astro.shared.core.utils.api.CentralApiEndpoint
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val log = KotlinLogging.logger { }

private const val UNKNOWN_INSTALL_SOURCE = "unknown"
private const val MAX_INSTALL_SOURCE_LENGTH = 64

@RestController
@RequestMapping
@Tag(name = "Authentication")
class AuthController(
    private val discordApiClient: DiscordApiClient,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val authSessionService: AuthSessionService,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val oAuthStateService: OAuthStateService,
    private val mcpOAuthService: McpOAuthService,
    private val frontendProperties: FrontendProperties,
    private val userRepository: UserRepository,
    private val bigQueryService: BigQueryService,
) {
    @Operation(
        operationId = "auth.login",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-forge-skip", value = "true", parseValue = true)])],
        summary = "Login via Discord",
        description =
            "Perform Discord OAuth flow, " +
                "will result in a session cookie on a successful flow + " +
                "a redirect to the provided `redirect_path` (if it contains `{guild_id}` " +
                "that will be replaced with the id of the guild the bot was added to.\n" +
                "Instead on a query parameter called `error_code` set in the baseUrl of the frontend on errors " +
                "(errors can be: discord_oauth_error, invalid_oauth_state, internal).\n" +
                "`source` is a free label of where an invite came from (for example topgg), " +
                "recorded as is with the install (without `:` and cut to 64 characters), unknown if missing.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "307",
            description = "Redirect to Discord authorization.",
        ),
    )
    @SecurityRequirements
    @GetMapping(CentralApiEndpoint.DISCORD_LOGIN)
    fun discord(
        @RequestParam("redirect_path") redirectPath: String,
        @RequestParam("invite", defaultValue = "false") invite: Boolean,
        @RequestParam("invite_guild_id", required = false) inviteGuildId: String? = null,
        @RequestParam("source", required = false) source: String? = null,
        @Parameter(hidden = true) @RequestParam("mcp_request_id", required = false) mcpRequestId: String? = null,
    ): ResponseEntity<Void> {
        val state =
            oAuthStateService.create(
                redirectPath,
                source?.replace(":", "")?.take(MAX_INSTALL_SOURCE_LENGTH)?.takeIf { it.isNotBlank() } ?: UNKNOWN_INSTALL_SOURCE,
                mcpRequestId,
            )

        val scopes =
            if (invite || inviteGuildId != null) {
                "identify email guilds bot applications.commands"
            } else {
                "identify email guilds"
            }

        val uri =
            UriComponentsBuilder
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
                }.build()
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
        hidden = true,
    )
    @SecurityRequirements
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

        val oAuthState =
            oAuthStateService.consume(state)
                ?: return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(URI("${frontendProperties.baseUrl}?error_code=invalid_oauth_state"))
                    .build()

        var redirectPath = oAuthState.redirectPath

        try {
            val discordToken = discordApiClient.getAccessToken(code, discordOAuthProperties)
            val discordUser = discordApiClient.getSelfUser(discordToken.accessToken)

            discordUserTokenPersistenceService.upsert(discordUser.id, discordToken)

            val authSession = authSessionService.createSession(discordUser.id)

            val cookie = authSessionService.createSessionCookie(authSession)

            val headers =
                HttpHeaders().apply {
                    set(HttpHeaders.SET_COOKIE, cookie.toString())
                }

            val guildId = discordToken.guild?.id
            if (guildId != null) {
                redirectPath = redirectPath.replace("{guild_id}", guildId)
                bigQueryService.push(
                    InstallEventData(
                        source = oAuthState.source,
                        guildId = guildId.toLong(),
                        userId = discordUser.id.toLong(),
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
                    ),
                )
            }

            if (oAuthState.mcpRequestId != null) {
                mcpOAuthService.setAuthRequestUser(oAuthState.mcpRequestId, discordUser.id)
                return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .location(
                        UriComponentsBuilder
                            .fromPath(CentralApiEndpoint.OAUTH_CONSENT)
                            .queryParam("request_id", oAuthState.mcpRequestId)
                            .build()
                            .toUri(),
                    ).headers(headers)
                    .build()
            }

            return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI("${frontendProperties.baseUrl}$redirectPath"))
                .headers(headers)
                .build()
        } catch (e: Exception) {
            if (e !is ABadRequestException) {
                log.error(e) { "Discord OAuth callback failed" }
            }

            return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI("${frontendProperties.baseUrl}?error_code=internal"))
                .build()
        }
    }

    @Operation(
        operationId = "auth.me",
        summary = "Get logged in user",
        description = "Returns the id, Discord username and avatar hash of the logged in user. Use it to check that the session is valid.",
    )
    @GetMapping(CentralApiEndpoint.ME)
    fun me(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<AuthUserResponse> {
        val user =
            userRepository.findByUserID(authPrincipal.userId)
                ?: run {
                    val userEntity = UserEntity(userID = authPrincipal.userId)
                    userRepository.save(userEntity)
                    userEntity
                }

        val discordUser = discordApiClient.getSelfUser(authPrincipal.userDiscordToken)

        return ResponseEntity.ok(
            AuthUserResponse(
                id = user.userID,
                username = discordUser.username,
                avatar = discordUser.avatar,
            ),
        )
    }

    @Operation(
        summary = "Logout",
        description = "Logout and delete the session cookie",
        operationId = "auth.logout",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-forge-skip", value = "true", parseValue = true)])],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "204",
            description = "Session deleted and session cookie expired.",
        ),
    )
    @PostMapping(CentralApiEndpoint.LOGOUT)
    fun logout(
        @AuthenticationPrincipal authPrincipal: AuthPrincipal,
    ): ResponseEntity<Void> {
        // MCP clients use the Discord token too, so it stays while the user has granted access to one
        if (!mcpOAuthService.hasActiveGrants(authPrincipal.userId)) {
            discordUserTokenPersistenceService.delete(authPrincipal.userId)
        }
        authSessionService.deleteSession(authPrincipal.userId, authPrincipal.sessionId)

        return ResponseEntity
            .noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                authSessionService.createExpiredSessionCookie().toString(),
            ).build()
    }
}
