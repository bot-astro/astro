package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.services.RateLimitingService

@Component
class IpRateLimitFilter(
    rateLimitingService: RateLimitingService,
    exceptionHandler: ControllersExceptionHandler,
) : BaseRateLimitFilter(rateLimitingService, exceptionHandler) {
    override fun resolveClientId(request: HttpServletRequest): String = request.remoteAddr
}
