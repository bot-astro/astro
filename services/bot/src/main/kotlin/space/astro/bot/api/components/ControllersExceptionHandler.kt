package space.astro.bot.api.components

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import space.astro.shared.core.exceptions.AErrorResponse
import space.astro.shared.core.exceptions.AException

@RestControllerAdvice
class ControllersExceptionHandler {

    @ExceptionHandler(AException::class)
    fun handleException(e: AException): ResponseEntity<AErrorResponse> {
        return ResponseEntity
            .status(e.httpStatusCode)
            .body(AErrorResponse(
                code = e.errorCode,
                message = e.message,
            ))
    }
}
