package space.astro.shared.core.exceptions

open class AException(
    val httpStatusCode: Int,
    val errorCode: AErrorCode,
    message: String,
    cause: Throwable? = null
): Exception(message, cause)

class AUnknownException(
    message: String,
    cause: Throwable? = null
): AException(
    httpStatusCode = 500,
    errorCode = AErrorCode.UNKNOWN,
    message = message,
    cause = cause
)

class ABadRequestException(
    message: String,
    cause: Throwable? = null
): AException(
    httpStatusCode = 400,
    errorCode = AErrorCode.INVALID_REQUEST,
    message = message,
    cause = cause
)