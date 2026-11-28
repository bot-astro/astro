package space.astro.shared.core.exceptions

open class AException(
    val httpStatusCode: Int,
    val errorCode: AErrorCode,
    message: String,
    cause: Throwable? = null,
    /** Name of the request field (or query parameter) that caused the error */
    val param: String? = null,
    /** Seconds to wait before retrying, sent as the Retry-After header */
    val retryAfterSeconds: Long? = null,
) : Exception(message, cause)

class AUnknownException(
    message: String,
    cause: Throwable? = null,
) : AException(
        httpStatusCode = 500,
        errorCode = AErrorCode.UNKNOWN,
        message = message,
        cause = cause,
    )

class ABadRequestException(
    message: String,
    cause: Throwable? = null,
    param: String? = null,
) : AException(
        httpStatusCode = 400,
        errorCode = AErrorCode.INVALID_REQUEST,
        message = message,
        cause = cause,
        param = param,
    )

class AUnauthorizedException(
    message: String,
    cause: Throwable? = null,
) : AException(
        httpStatusCode = 403,
        errorCode = AErrorCode.UNAUTHORIZED,
        message = message,
        cause = cause,
    )

class ANotFoundException(
    message: String,
    cause: Throwable? = null,
) : AException(
        httpStatusCode = 404,
        errorCode = AErrorCode.NOT_FOUND,
        message = message,
        cause = cause,
    )

class APremiumRequiredException(
    message: String,
    cause: Throwable? = null,
) : AException(
        httpStatusCode = 403,
        errorCode = AErrorCode.PREMIUM_REQUIRED,
        message = message,
        cause = cause,
    )

class ATooManyRequestsException(
    message: String,
    retryAfterSeconds: Long,
    cause: Throwable? = null,
) : AException(
        httpStatusCode = 429,
        errorCode = AErrorCode.TOO_MANY_REQUESTS,
        message = message,
        cause = cause,
        retryAfterSeconds = retryAfterSeconds,
    )

class AInsufficientScopeException(
    requiredScope: String,
) : AException(
        httpStatusCode = 403,
        errorCode = AErrorCode.INSUFFICIENT_SCOPE,
        message = "The access token is missing the scope $requiredScope",
    )

class AIdempotencyInProgressException :
    AException(
        httpStatusCode = 409,
        errorCode = AErrorCode.IDEMPOTENCY_IN_PROGRESS,
        message = "A request with the same Idempotency-Key is still being processed, retry shortly",
        retryAfterSeconds = 1,
    )

class AIdempotencyKeyReusedException :
    AException(
        httpStatusCode = 422,
        errorCode = AErrorCode.IDEMPOTENCY_KEY_REUSED,
        message = "This Idempotency-Key was already used with a different request, use a new key for a new request",
        param = "Idempotency-Key",
    )
