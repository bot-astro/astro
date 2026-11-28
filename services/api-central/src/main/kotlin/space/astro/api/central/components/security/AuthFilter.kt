package space.astro.api.central.components.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.models.auth.AuthContext
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.models.auth.AuthScope
import space.astro.api.central.services.AuthSessionService
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.McpOAuthService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.properties.api_central.CentralApiProperties

@Component
class AuthFilter(
    private val authSessionService: AuthSessionService,
    private val mcpOAuthService: McpOAuthService,
    private val centralApiProperties: CentralApiProperties,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val discordApiClient: DiscordApiClient,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            authenticate(request)
        } catch (e: Exception) {
            // the context will have no authenticated user
            // so it will fail in the securityFilterChain
            logger.debug("Authentication failed, continuing as anonymous", e)
            SecurityContextHolder.clearContext()
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(request: HttpServletRequest) {
        val bearerToken =
            request
                .getHeader(HttpHeaders.AUTHORIZATION)
                ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                ?.substring("Bearer ".length)
                ?.trim()

        val cookieValue =
            request.cookies
                ?.firstOrNull { it.name == centralApiProperties.sessionCookieName }
                ?.value

        // (userId, sessionId), for the tokens of MCP clients the session id is the id of the token
        // the dashboard session can do everything, the tokens of MCP clients only what their scopes allow
        var scopes = AuthScope.ALL
        val identity: Pair<String, String> =
            if (bearerToken != null) {
                val token = mcpOAuthService.getAccessToken(bearerToken) ?: return
                scopes = token.scopes
                Pair(token.userId, token.tokenId)
            } else if (cookieValue != null) {
                val (userId, sessionId) = authSessionService.getUserAndSessionIdFromCookie(cookieValue)
                val sessionData = authSessionService.getSession(userId, sessionId) ?: return
                Pair(sessionData.userId, sessionData.sessionId)
            } else {
                return
            }
        val (userId, sessionId) = identity

        val (isExpired, userDiscordToken) =
            discordUserTokenPersistenceService.get(userId)
                ?: return

        val discordAccessToken =
            if (!isExpired) {
                userDiscordToken.accessToken
            } else {
                val discordTokenDto =
                    discordApiClient.refreshToken(userDiscordToken.refreshToken, discordOAuthProperties)
                discordUserTokenPersistenceService.upsert(userId, discordTokenDto)
                discordTokenDto.accessToken
            }

        val authContext =
            AuthContext(
                authPrincipal = AuthPrincipal(sessionId, userId, discordAccessToken),
                authorities = scopes.map { SimpleGrantedAuthority(it) },
            )
        SecurityContextHolder.getContext().authentication = authContext
    }
}
