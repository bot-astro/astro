package space.astro.api.central.services

import com.chargebee.v4.models.subscription.Subscription
import org.springframework.stereotype.Service
import space.astro.api.central.models.responses.UserSubscriptionsResponse
import space.astro.shared.core.clients.ChargebeeClientHelper
import space.astro.shared.core.models.database.UserEntity
import space.astro.shared.core.repositories.mongo.UserRepository

/**
 * Combines the Chargebee subscriptions of a user with the guilds that the user upgraded with them
 */
@Service
class UserSubscriptionsService(
    private val chargebeeClientHelper: ChargebeeClientHelper,
    private val userRepository: UserRepository,
) {
    fun getOrCreateUser(userId: String): UserEntity =
        userRepository.findByUserID(userId) ?: userRepository.save(UserEntity(userID = userId))

    fun getSubscriptionsInfo(user: UserEntity) =
        UserSubscriptionsResponse(
            upgradedGuilds =
                user.guildActiveUpgrades.map {
                    UserSubscriptionsResponse.UpgradedGuild(
                        subscriptionId = it.subscriptionID,
                        guildId = it.guildID,
                    )
                },
            subscriptions = getActiveSubscriptions(user),
        )

    fun getActiveSubscriptions(user: UserEntity): List<UserSubscriptionsResponse.UserSubscription> =
        chargebeeClientHelper.getActiveServerSubscriptionsOfUser(user.userID).map {
            val subscription = it.subscription
            val quantities = subscription.subscriptionItems?.firstOrNull()?.quantity ?: 0
            val used = user.guildActiveUpgrades.count { upgrade -> upgrade.subscriptionID == subscription.id }

            UserSubscriptionsResponse.UserSubscription(
                subscriptionId = subscription.id,
                annual = subscription.billingPeriodUnit == Subscription.BillingPeriodUnit.YEAR,
                quantities = quantities,
                used = used,
                available = quantities - used,
            )
        }
}
