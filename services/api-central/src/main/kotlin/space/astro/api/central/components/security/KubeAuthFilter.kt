package space.astro.api.central.components.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.shared.core.exceptions.AUnauthorizedException
import space.astro.shared.core.properties.KubeProperties

@Component
class KubeAuthFilter(
    private val kubeProperties: KubeProperties
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !request.requestURI.startsWith("/actuator")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authKey = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (authKey != kubeProperties.lifecycleAuthKey) {
            throw AUnauthorizedException("Invalid auth secret key provided for actuator endpoint")
        }

        filterChain.doFilter(request, response)
    }
}