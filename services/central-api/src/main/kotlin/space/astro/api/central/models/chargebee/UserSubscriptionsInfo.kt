package space.astro.api.central.models.chargebee

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserSubscriptionsInfo(
    val upgradedGuilds: List<UpgradedGuildInfo>,
    val subscriptions: List<UserSubscription>
)

data class UserSubscription(
    val subscriptionId: String,
    val quantities: Int
)

data class UpgradedGuildInfo(
    val subscriptionId: String,
    val guildId: String
)