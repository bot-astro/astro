package space.astro.api.central.models.requests

/**
 * Subset of the Chargebee subscription events payload that we care about
 */
data class ChargebeeWebhookBody(
    val content: Content,
) {
    data class Content(
        val customer: Customer,
        val subscription: Subscription,
    )

    data class Customer(
        val id: String,
    )

    data class Subscription(
        val id: String,
        val subscriptionItems: List<SubscriptionItem> = emptyList(),
    )

    data class SubscriptionItem(
        val itemPriceId: String,
    )
}
