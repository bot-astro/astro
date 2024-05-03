package space.astro.shared.core.services.chargebee

import com.chargebee.Environment
import com.chargebee.ListResult
import com.chargebee.models.Customer
import com.chargebee.models.PortalSession
import com.chargebee.models.Subscription
import org.springframework.stereotype.Service
import space.astro.shared.core.configs.ChargebeeConfig

/**
 * API client to interact with Chargebee
 *
 * @see createPortalSession
 */
@Service
class ChargebeeClientService(
    chargebeeConfig: ChargebeeConfig
) {
    companion object {
        /**
         * The id of the server premium plan
         */
        private const val SERVER_PREMIUM_PLAN_ID = "Server-Premium"
    }

    init {
        // Configure chargebee site name and api key globally
        Environment.configure(
            chargebeeConfig.siteName,
            chargebeeConfig.apiKey
        )
    }

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
            val result = PortalSession.create()
                .customerId(userID)
                .request()

            return result.portalSession().accessUrl()
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
     * Fetches all user subscriptions for [SERVER_PREMIUM_PLAN_ID]
     *
     * @param userID
     *
     * @return the list of subscriptions
     */
    fun getServerSubscriptionsOfUser(userID: String): ListResult =
        Subscription.list()
            .customerId().`is`(userID)
            .itemPriceId().startsWith(SERVER_PREMIUM_PLAN_ID)
            .request()

    /**
     * Fetches all active user subscriptions for [SERVER_PREMIUM_PLAN_ID]
     *
     * @param userID
     *
     * @return the list of active subscriptions
     */
    fun getActiveServerSubscriptionsOfUser(userID: String): List<ListResult.Entry> =
        getServerSubscriptionsOfUser(userID).filter {
            it.subscription().status() != Subscription.Status.CANCELLED && !it.subscription().deleted()
        }

    /**
     * Creates a user with the provided [userID] in Chargebee
     *
     * @param userID
     */
    private fun createUser(userID: String) {
        Customer.create()
            .id(userID)
            .request()
    }
}