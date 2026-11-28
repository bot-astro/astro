package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import space.astro.api.central.components.ControllersExceptionHandler
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.api.central.services.RateLimitingService

/**
 * Rate limit of authenticated requests, shared by the dashboard session and the MCP clients of the user
 */
@Component
class UserIdRateLimitFilter(
    rateLimitingService: RateLimitingService,
    exceptionHandler: ControllersExceptionHandler,
) : BaseRateLimitFilter(rateLimitingService, exceptionHandler) {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = super.shouldNotFilter(request) || currentUserId() == null

    // prefixed so a user id can't share the bucket of an IP
    override fun resolveClientId(request: HttpServletRequest): String = "user:${currentUserId()}"

    private fun currentUserId(): String? = (SecurityContextHolder.getContext().authentication?.principal as? AuthPrincipal)?.userId
}
