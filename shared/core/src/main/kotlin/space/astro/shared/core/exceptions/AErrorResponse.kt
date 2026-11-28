package space.astro.shared.core.exceptions

data class AErrorResponse(
    val code: AErrorCode,
    val message: String? = null,
    /** Name of the request field or query parameter that caused the error, when there is one */
    val param: String? = null,
    /** Id of the failed request, to give to support or to find it in the logs */
    val requestId: String? = null,
)
