package space.astro.api.central.components.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.models.auth.AuthContext
import space.astro.api.central.services.dashboard.AuthSessionService
import space.astro.shared.core.properties.api_central.CentralApiProperties

@Component
class AuthFilter(
    private val authSessionService: AuthSessionService,
    private val centralApiProperties: CentralApiProperties
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

            if (sessionData != null) {
                val authContext = AuthContext(
                    sessionData = sessionData,
                    authorities = emptyList()
                )
                SecurityContextHolder.getContext().authentication = authContext
            }
        }

        filterChain.doFilter(request, response)
    }
}