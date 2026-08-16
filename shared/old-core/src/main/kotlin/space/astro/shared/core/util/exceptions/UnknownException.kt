package space.astro.shared.core.util.exceptions

class UnknownException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Exception(message, cause)