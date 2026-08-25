package space.astro.api.central.models.chargebee

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubscriptionWebhookData(
    val content: CBContent
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CBContent(
    val customer: CBCustomer,
    val subscription: CBSubscription
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CBCustomer(
    val id: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CBSubscription(
    val id: String,
    val subscription_items: MutableList<CBSubscriptionItem>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CBSubscriptionItem(
    val item_price_id: String
)
