package space.astro.api.central.models.chargebee

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CheckoutBody(
    val monthly: Boolean,
    val quantity: Int
)
