package space.astro.api.central.components.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.models.auth.AuthContext
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.DiscordUserTokenPersistenceService
import space.astro.api.central.services.AuthSessionService
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.properties.DiscordOAuthProperties
import space.astro.shared.core.properties.KubeProperties
import space.astro.shared.core.properties.api_central.CentralApiProperties

@Component
class AuthFilter(
    private val authSessionService: AuthSessionService,
    private val exceptionHandler: ControllersExceptionHandler,
    private val centralApiProperties: CentralApiProperties,
    private val discordOAuthProperties: DiscordOAuthProperties,
    private val discordUserTokenPersistenceService: DiscordUserTokenPersistenceService,
    private val discordApiClient: DiscordApiClient,
    private val kubeProperties: KubeProperties
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            authenticate(request)
        } catch (e: Exception) {
            SecurityContextHolder.clearContext()
            exceptionHandler.writeException(response, e)
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(request: HttpServletRequest) {
        if (request.requestURI.startsWith("/actuator")) {
            val authKey = request.getHeader(HttpHeaders.AUTHORIZATION)

            if (authKey != kubeProperties.lifecycleAuthKey) {
                throw AUnauthorizedException("Invalid auth secret key provided for actuator endpoint")
            }

            val authContext = AuthContext(
                authPrincipal = AuthPrincipal("kube_no_session_id", "kube_no_user_id", "kube_no_token"),
                authorities = emptyList()
            )
            SecurityContextHolder.getContext().authentication = authContext

            return
        }

        val cookieValue = request.cookies
            ?.firstOrNull { it.name == centralApiProperties.sessionCookieName }
            ?.value

        if (cookieValue != null) {
            val (userId, sessionId) = authSessionService.getUserAndSessionIdFromCookie(cookieValue)
            val sessionData = authSessionService.getSession(userId, sessionId)
                ?: return

            val (isExpired, userDiscordToken) = discordUserTokenPersistenceService.get(sessionData.userId)
                ?: return

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
    }
}