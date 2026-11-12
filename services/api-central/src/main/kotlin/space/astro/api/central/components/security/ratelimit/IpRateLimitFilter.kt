package space.astro.api.central.components.security.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import space.astro.api.central.services.RateLimitingService
import tools.jackson.databind.json.JsonMapper

@Component
class IpRateLimitFilter(
    rateLimitingService: RateLimitingService,
    jsonMapper: JsonMapper,
) : BaseRateLimitFilter(rateLimitingService, jsonMapper) {
    override fun resolveClientId(request: HttpServletRequest): String = request.remoteAddr
}
