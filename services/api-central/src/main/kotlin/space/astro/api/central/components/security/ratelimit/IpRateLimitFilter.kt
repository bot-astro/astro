package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.RateLimitingService

/**
 * Rate limit of anonymous requests, the authenticated ones are limited by [UserIdRateLimitFilter]
 */
@Component
class IpRateLimitFilter(
    rateLimitingService: RateLimitingService,
    exceptionHandler: ControllersExceptionHandler,
) : BaseRateLimitFilter(rateLimitingService, exceptionHandler) {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        super.shouldNotFilter(request) || SecurityContextHolder.getContext().authentication?.principal is AuthPrincipal

    override fun resolveClientId(request: HttpServletRequest): String = request.remoteAddr
}
