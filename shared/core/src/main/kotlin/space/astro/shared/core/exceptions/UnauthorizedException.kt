package space.astro.shared.core.exceptions

class UnauthorizedException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)