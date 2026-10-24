package space.astro.support.bot.api.components.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.shared.core.properties.KubeProperties
import space.astro.support.bot.properties.SupportBotApiProperties

/**
 * Authenticates the requests through the Authorization header:
 * - actuator endpoints (called by Kubernetes) expect [KubeProperties.lifecycleAuthKey]
 * - everything else (called by the other services) expects [SupportBotApiProperties.auth]
 *
 * Requests with a wrong key continue as anonymous and are rejected by the security filter chain.
 */
@Component
class AuthFilter(
    private val kubeProperties: KubeProperties,
    private val supportBotApiProperties: SupportBotApiProperties,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val expectedKey =
            if (request.requestURI.startsWith("/actuator")) {
                kubeProperties.lifecycleAuthKey
            } else {
                supportBotApiProperties.auth
            }

        val providedKey = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (providedKey == expectedKey) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken("service", null, emptyList())
        }

        filterChain.doFilter(request, response)
    }
}
