package space.astro.api.central.components.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.properties.AiProperties
import java.security.MessageDigest

/**
 * Authenticates the internal requests of the other services through the [HEADER_NAME] header,
 * which must match [AiProperties.internalToken]. An empty token rejects every request.
 *
 * Requests with a wrong token continue as anonymous and are rejected by the security filter chain.
 */
@Component
class InternalAuthFilter(
    private val aiProperties: AiProperties,
) : OncePerRequestFilter() {
    companion object {
        const val HEADER_NAME = "X-Internal-Token"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val providedToken = request.getHeader(HEADER_NAME)

        if (aiProperties.internalToken.isNotEmpty() &&
            providedToken != null &&
            MessageDigest.isEqual(providedToken.toByteArray(), aiProperties.internalToken.toByteArray())
        ) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken("service", null, emptyList())
        }

        filterChain.doFilter(request, response)
    }
}
