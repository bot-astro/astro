package space.astro.shared.core.exceptions

data class AErrorResponse(
    val code: AErrorCode,
    val message: String? = null,
)