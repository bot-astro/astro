package space.astro.shared.core.services.chargebee

import com.chargebee.ListResult
import com.chargebee.models.Coupon
import com.chargebee.models.Customer
import com.chargebee.models.PortalSession
import com.chargebee.models.Subscription
import org.springframework.stereotype.Service
import space.astro.shared.core.configs.ChargebeeConfig
import java.util.*

@Service
class ChargebeeClientService(
    private val chargebeeConfig: ChargebeeConfig
) {
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

    private fun getUserSubscriptions(userID: String): ListResult =
        Subscription.list()
            .customerId().`is`(userID)
            .itemPriceId().startsWith("Server-Premium")
            .request()

    fun getUserActiveSubscriptions(userID: String): List<ListResult.Entry> =
        getUserSubscriptions(userID).filter {
            it.subscription().status() != Subscription.Status.CANCELLED && !it.subscription().deleted()
        }

    fun hasActiveSubscription(userID: String):Boolean {
        return Subscription.list()
            .customerId().`is`(userID)
            .request()
            .any { it.subscription().status() != Subscription.Status.CANCELLED && !it.subscription().deleted() }
    }

    fun createOneMonthCoupon(userID: String): Coupon? {
        try {
            val result = Coupon.createForItems()
                .id("${userID}_${UUID.randomUUID()}".take(99))
                .name("Coins 1 month $userID")
                .durationType(Coupon.DurationType.ONE_TIME)
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountAmount(399)
                .applyOn(Coupon.ApplyOn.INVOICE_AMOUNT)
                .maxRedemptions(1)
                .request()

            return result.coupon()
        } catch (e: Exception) {
            // User already created a coupon with its account
            return null
        }
    }

    fun createUserPremiumCoupon(userID: String): Coupon? {
        try {
            val result = Coupon.createForItems()
                .id("${userID}_${UUID.randomUUID()}".take(99))
                .name("Coins 1 user premium month $userID")
                .durationType(Coupon.DurationType.ONE_TIME)
                .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                .discountAmount(99)
                .applyOn(Coupon.ApplyOn.INVOICE_AMOUNT)
                .maxRedemptions(1)
                .request()

            return result.coupon()
        } catch (e: Exception) {
            // User already created a coupon with its account
            return null
        }
    }

    private fun createUser(userID: String) {
        Customer.create()
            .id(userID)
            .request()
    }
}