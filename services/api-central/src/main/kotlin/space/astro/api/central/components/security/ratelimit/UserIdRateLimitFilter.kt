package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.RateLimitingService

@Component
class UserIdRateLimitFilter(
    rateLimitingService: RateLimitingService,
    exceptionHandler: ControllersExceptionHandler,
) : BaseRateLimitFilter(rateLimitingService, exceptionHandler) {
    override fun resolveClientId(request: HttpServletRequest): String {
        val auth = SecurityContextHolder.getContext().authentication
        val userId = (auth?.principal as? AuthPrincipal)?.userId

        return userId ?: request.remoteAddr
    }
}
