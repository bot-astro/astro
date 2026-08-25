package space.astro.shared.core.clients

import com.chargebee.v4.client.ChargebeeClient
import com.chargebee.v4.models.customer.params.CustomerCreateParams
import com.chargebee.v4.models.portalSession.params.PortalSessionCreateParams
import com.chargebee.v4.models.subscription.Subscription
import com.chargebee.v4.models.subscription.params.SubscriptionListParams
import com.chargebee.v4.models.subscription.responses.SubscriptionListResponse
import space.astro.shared.core.properties.ChargebeeClientProperties

class ChargebeeClientHelper(
    private val config: ChargebeeClientProperties
) {
    val client: ChargebeeClient = ChargebeeClient.builder()
        .apiKey(config.apiKey)
        .siteName(config.siteName)
        .build()

    /**
     * Creates a portal session for the user with the provided [userID]
     *
     * @param userID
     * @param retry if the request fails and this parameter is set to true, it will try to create the user in Chargebee and then re-perform the request
     *
     * @return the access url for the portal session or null if it failed
     */
    fun createPortalSession(userID: String, retry: Boolean = true): String? {
        return try {
            val res = client.portalSessions().create(
                PortalSessionCreateParams.builder()
                    .customer(PortalSessionCreateParams.CustomerParams.builder()
                        .id(userID)
                        .build()
                    )
                    .build()
            )

            return res.portalSession.accessUrl
        } catch (e: Exception) {
            if (retry) {
                createUser(userID)
                createPortalSession(userID, false)
            } else {
                null
            }
        }
    }

    /**
     * Fetches all user subscriptions for [ChargebeeClientProperties.serverUltimatePlanId]
     *
     * @param userID
     *
     * @return the list of subscriptions
     */
    fun getServerSubscriptionsOfUser(userID: String): List<SubscriptionListResponse.SubscriptionListItem> {
        val res = client.subscriptions().list(
            SubscriptionListParams.builder()
                .customerId().`is`(userID)
                .itemPriceId().startsWith(config.serverUltimatePlanId)
                .build()
        )
        return res.list
    }

    /**
     * Fetches all active user subscriptions for [ChargebeeClientProperties.serverUltimatePlanId]
     *
     * @param userID
     *
     * @return the list of active subscriptions
     */
    fun getActiveServerSubscriptionsOfUser(userID: String): List<SubscriptionListResponse.SubscriptionListItem> =
        getServerSubscriptionsOfUser(userID).filter {
            it.subscription.status != Subscription.Status.CANCELLED && !it.subscription.deleted
        }

    /**
     * Creates a user with the provided [userID] in Chargebee
     *
     * @param userID
     */
    private fun createUser(userID: String) {
        client.customers().create(
            CustomerCreateParams.builder()
                .id(userID)
                .build()
        )
    }
}