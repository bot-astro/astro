package space.astro.api.central.components

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * Gives every request an id, put in the logs (MDC), in the `X-Request-Id` response header and in the error responses.
 *
 * ingress-nginx already generates an `X-Request-Id` for each request it forwards, it is reused so that
 * the ingress access logs and the application logs can be matched. Ids that are not made of a safe
 * charset are replaced, they come from the client and end up in the logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    companion object {
        const val HEADER = "X-Request-Id"
        const val MDC_KEY = "requestId"

        private val VALID_ID = Regex("^[A-Za-z0-9._-]{8,128}$")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId =
            request.getHeader(HEADER)?.takeIf { VALID_ID.matches(it) }
                ?: "req_${UUID.randomUUID().toString().replace("-", "")}"

        MDC.put(MDC_KEY, requestId)
        response.setHeader(HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}
