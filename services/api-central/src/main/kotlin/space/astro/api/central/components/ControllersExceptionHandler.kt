package space.astro.api.central.components

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.TypeMismatchException
import org.springframework.expression.ExpressionInvocationTargetException
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
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AErrorResponse
import space.astro.shared.core.exceptions.AException
import tools.jackson.databind.json.JsonMapper
import java.io.IOException

private val log = KotlinLogging.logger { }

private val CLIENT_DISCONNECT_MESSAGES = setOf("Broken pipe", "Connection reset by peer")

@RestControllerAdvice
class ControllersExceptionHandler(
    private val jsonMapper: JsonMapper,
) {
    private data class Resolved(
        val status: Int,
        val code: AErrorCode,
        val message: String,
        val headers: HttpHeaders = HttpHeaders.EMPTY,
    )

    @ExceptionHandler(Exception::class)
    fun handleException(thrown: Exception): ResponseEntity<AErrorResponse> {
        // SpEL (@PreAuthorize) wraps the exceptions thrown by the beans it invokes
        val e =
            generateSequence<Throwable>(thrown) { it.cause }
                .filterIsInstance<ExpressionInvocationTargetException>()
                .firstOrNull()
                ?.cause as? Exception
                ?: thrown
        val causes = generateSequence<Throwable>(e) { it.cause }.toList()

        // the client went away mid-response, nobody is listening and writing an error body would just fail again
        if (causes.any { it is AsyncRequestNotUsableException || (it is IOException && it.message in CLIENT_DISCONNECT_MESSAGES) }) {
            log.debug { "Client disconnected before the response was written: ${e.message}" }
            return ResponseEntity.status(499).build()
        }

        // an internal service (e.g. a bot pod being restarted or rolled out) can't be reached, usually transient
        if (causes.any { it is ResourceAccessException }) {
            log.warn(e) { "Upstream service unreachable" }
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(AErrorResponse(code = AErrorCode.UNKNOWN, message = "Service temporarily unavailable, try again shortly."))
        }

        val r = resolve(e)
        if (r.status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(e) { "API request failed" }
        }
        return ResponseEntity
            .status(r.status)
            .headers(r.headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body(AErrorResponse(code = r.code, message = r.message))
    }

    private fun resolve(e: Exception): Resolved =
        when (e) {
            is AException ->
                Resolved(e.httpStatusCode, e.errorCode, e.message ?: "Request failed.")

            is AuthenticationException ->
                Resolved(HttpStatus.UNAUTHORIZED.value(), AErrorCode.UNAUTHENTICATED, "Authentication is required.")

            is AccessDeniedException ->
                Resolved(HttpStatus.FORBIDDEN.value(), AErrorCode.UNAUTHORIZED, "Access denied.")

            is MethodArgumentTypeMismatchException ->
                Resolved(HttpStatus.BAD_REQUEST.value(), AErrorCode.INVALID_REQUEST, "Invalid value for parameter '${e.name}'.")

            is MissingServletRequestParameterException ->
                Resolved(
                    HttpStatus.BAD_REQUEST.value(),
                    AErrorCode.INVALID_REQUEST,
                    "Required parameter '${e.parameterName}' is missing or empty.",
                )

            is HttpMessageNotReadableException ->
                Resolved(HttpStatus.BAD_REQUEST.value(), AErrorCode.INVALID_REQUEST, "Invalid request body.")

            is TypeMismatchException ->
                Resolved(HttpStatus.BAD_REQUEST.value(), AErrorCode.INVALID_REQUEST, "Invalid request parameter.")

            is ErrorResponse -> {
                val status = e.statusCode.value()
                Resolved(
                    status,
                    codeForStatus(status),
                    HttpStatus.resolve(status)?.reasonPhrase ?: "Request failed.",
                    e.headers,
                )
            }

            else -> Resolved(HttpStatus.INTERNAL_SERVER_ERROR.value(), AErrorCode.UNKNOWN, "An unexpected server error occurred.")
        }

    private fun codeForStatus(status: Int): AErrorCode =
        when (status) {
            HttpStatus.UNAUTHORIZED.value() -> AErrorCode.UNAUTHENTICATED
            HttpStatus.FORBIDDEN.value() -> AErrorCode.UNAUTHORIZED
            HttpStatus.NOT_FOUND.value() -> AErrorCode.NOT_FOUND
            in HttpStatus.BAD_REQUEST.value()..<HttpStatus.INTERNAL_SERVER_ERROR.value() -> AErrorCode.INVALID_REQUEST
            else -> AErrorCode.UNKNOWN
        }

    // Filters run outside controller advice, so use the same response mapping there.
    fun writeException(
        response: HttpServletResponse,
        e: Exception,
    ) {
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
