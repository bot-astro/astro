package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.services.RateLimitingService
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AErrorResponse
import tools.jackson.databind.json.JsonMapper
import kotlin.math.max
import kotlin.time.Duration.Companion.nanoseconds

abstract class BaseRateLimitFilter(
    private val rateLimitingService: RateLimitingService,
    private val jsonMapper: JsonMapper
) : OncePerRequestFilter() {

    /**
     * Resolves the identifier used to select the rate-limit bucket.
     */
    protected abstract fun resolveClientId(
        request: HttpServletRequest
    ): String

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
       return request.requestURI.startsWith("/actuator")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientId = resolveClientId(request)
        val bucket = rateLimitingService.resolveBucket(clientId)
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        response.setHeader("X-Rate-Limit-Limit", RateLimiterConfiguration.BUCKET_CAPACITY.toString())
        response.setHeader(
            "X-Rate-Limit-Remaining",
            probe.remainingTokens.toString()
        )

        if (!probe.isConsumed) {
            val retryAfterSeconds = max(
                1,
                probe.nanosToWaitForRefill.nanoseconds.inWholeSeconds
            )

            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.setHeader(
                "Retry-After",
                retryAfterSeconds.toString()
            )
            response.contentType = MediaType.APPLICATION_JSON_VALUE

            val errorResponse = AErrorResponse(
                code = AErrorCode.TOO_MANY_REQUESTS,
                message = "Rate limited, try again in $retryAfterSeconds seconds"
            )

            jsonMapper.writeValue(
                response.writer,
                errorResponse
            )

            return
        }

        filterChain.doFilter(request, response)
    }
}