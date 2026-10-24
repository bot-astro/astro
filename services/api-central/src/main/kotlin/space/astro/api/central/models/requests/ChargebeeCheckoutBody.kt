package space.astro.api.central.models.requests

data class ChargebeeCheckoutBody(
    val monthly: Boolean,
    val quantity: Int,
)
