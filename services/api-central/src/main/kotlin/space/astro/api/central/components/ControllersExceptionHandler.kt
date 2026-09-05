package space.astro.api.central.components

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AErrorResponse
import space.astro.shared.core.exceptions.AException
import tools.jackson.databind.json.JsonMapper

private val log = KotlinLogging.logger { }

@RestControllerAdvice
class ControllersExceptionHandler(private val jsonMapper: JsonMapper) {

    private data class Resolved(
        val status: Int,
        val code: AErrorCode,
        val message: String,
        val headers: HttpHeaders = HttpHeaders.EMPTY,
    )

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<AErrorResponse> {
        val r = resolve(e)
        if (r.status >= 500) {
            log.error(e) { "API request failed" }
        }
        return ResponseEntity.status(r.status)
            .headers(r.headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(AErrorResponse(code = r.code, message = r.message))
    }

    private fun resolve(e: Exception): Resolved = when (e) {
        is AException ->
            Resolved(e.httpStatusCode, e.errorCode, e.message ?: "Request failed.")

        is AuthenticationException ->
            Resolved(401, AErrorCode.UNAUTHORIZED, "Authentication is required.")

        is AccessDeniedException ->
            Resolved(403, AErrorCode.UNAUTHORIZED, "Access denied.")

        is MethodArgumentTypeMismatchException ->
            Resolved(400, AErrorCode.INVALID_REQUEST, "Invalid value for parameter '${e.name}'.")

        is MissingServletRequestParameterException ->
            Resolved(400, AErrorCode.INVALID_REQUEST, "Required parameter '${e.parameterName}' is missing or empty.")

        is HttpMessageNotReadableException ->
            Resolved(400, AErrorCode.INVALID_REQUEST, "Invalid request body.")

        is TypeMismatchException ->
            Resolved(400, AErrorCode.INVALID_REQUEST, "Invalid request parameter.")

        is ErrorResponse -> {
            val status = e.statusCode.value()
            Resolved(status, codeForStatus(status), HttpStatus.resolve(status)?.reasonPhrase ?: "Request failed.", e.headers)
        }

        else -> Resolved(500, AErrorCode.UNKNOWN, "An unexpected server error occurred.")
    }

    private fun codeForStatus(status: Int): AErrorCode = when (status) {
        401, 403 -> AErrorCode.UNAUTHORIZED
        404 -> AErrorCode.NOT_FOUND
        in 400..499 -> AErrorCode.INVALID_REQUEST
        else -> AErrorCode.UNKNOWN
    }

    // Filters run outside controller advice, so use the same response mapping there.
    fun writeException(response: HttpServletResponse, e: Exception) {
        if (response.isCommitted) throw e

        val error = handleException(e)
        response.resetBuffer()
        response.status = error.statusCode.value()
        error.headers.forEach { name, values ->
            values.forEach { response.addHeader(name, it) }
        }
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(jsonMapper.writeValueAsString(error.body))
    }
}