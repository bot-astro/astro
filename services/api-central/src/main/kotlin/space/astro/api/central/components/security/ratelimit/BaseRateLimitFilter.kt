package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.services.RateLimitingService
import space.astro.shared.core.exceptions.ATooManyRequestsException
import kotlin.math.max
import kotlin.time.Duration.Companion.nanoseconds

abstract class BaseRateLimitFilter(
    private val rateLimitingService: RateLimitingService,
    private val exceptionHandler: ControllersExceptionHandler,
) : OncePerRequestFilter() {
    /**
     * Resolves the identifier used to select the rate-limit bucket.
     */
    protected abstract fun resolveClientId(request: HttpServletRequest): String

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.startsWith("/actuator")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientId = resolveClientId(request)
        val bucket = rateLimitingService.resolveApiGlobalBucket(clientId)
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        response.setHeader("X-Rate-Limit-Limit", RateLimitingService.BUCKET_API_GLOBAL_CAPACITY.toString())
        response.setHeader(
            "X-Rate-Limit-Remaining",
            probe.remainingTokens.toString(),
        )

        if (!probe.isConsumed) {
            val retryAfterSeconds =
                max(
                    1,
                    probe.nanosToWaitForRefill.nanoseconds.inWholeSeconds,
                )

            exceptionHandler.writeException(
                response,
                ATooManyRequestsException("Rate limited, try again in $retryAfterSeconds seconds", retryAfterSeconds),
            )
            return
        }

        filterChain.doFilter(request, response)
    }
}
