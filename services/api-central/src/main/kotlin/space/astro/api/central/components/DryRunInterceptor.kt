package space.astro.api.central.components

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import space.astro.shared.core.exceptions.ABadRequestException

/**
 * Marks the endpoints that accept `dryRun=true`: they run all the checks and return the result the request would
 * have without saving or doing anything, so the endpoint must stop before every write and side effect.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SupportsDryRun

/**
 * Rejects `dryRun=true` on the endpoints that don't support it, they would otherwise ignore it and do the real change
 */
@Component
class DryRunInterceptor :
    HandlerInterceptor,
    WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(this)
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (request.getParameter("dryRun") == "true" &&
            (handler as? HandlerMethod)?.hasMethodAnnotation(SupportsDryRun::class.java) != true
        ) {
            throw ABadRequestException("dryRun is not supported by this endpoint, nothing was changed.", param = "dryRun")
        }
        return true
    }
}
