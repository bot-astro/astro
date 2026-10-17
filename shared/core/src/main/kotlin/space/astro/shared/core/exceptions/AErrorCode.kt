package space.astro.shared.core.exceptions

enum class AErrorCode {
    UNKNOWN,
    INVALID_REQUEST,
    UNAUTHENTICATED,
    UNAUTHORIZED,
    NOT_FOUND,
    TOO_MANY_REQUESTS,

    TRANSLATION,
    BOT_NOT_IN_GUILD,
}
