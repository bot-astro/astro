package space.astro.api.central.components.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.models.auth.AuthContext
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.AuthSessionService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.properties.api_central.CentralApiProperties

@Component
class AuthFilter(
    private val authSessionService: AuthSessionService,
    private val centralApiProperties: CentralApiProperties,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val discordApiClient: DiscordApiClient
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val cookieValue = request.cookies
            ?.firstOrNull { it.name == centralApiProperties.sessionCookieName }
            ?.value

        if (cookieValue != null) {
            val (userId, sessionId) = authSessionService.getUserAndSessionIdFromCookie(cookieValue)
            val sessionData = authSessionService.getSession(userId, sessionId)
                ?: return filterChain.doFilter(request, response)

            val (isExpired, userDiscordToken) = discordUserTokenPersistenceService.get(sessionData.userId)
                ?: return filterChain.doFilter(request, response)

            val discordAccessToken = if (!isExpired) {
                userDiscordToken.accessToken
            } else {
                val discordTokenDto = discordApiClient.refreshToken(userDiscordToken.refreshToken, discordOAuthProperties)
                discordUserTokenPersistenceService.upsert(userId, discordTokenDto)
                discordTokenDto.accessToken
            }

            val authContext = AuthContext(
                authPrincipal = AuthPrincipal(sessionData.sessionId, sessionData.userId, discordAccessToken),
                authorities = emptyList()
            )
            SecurityContextHolder.getContext().authentication = authContext

        }

        filterChain.doFilter(request, response)
    }
}