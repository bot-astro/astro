package space.astro.api.central.models.responses

data class UserSubscriptionsResponse(
    val upgradedGuilds: List<UpgradedGuild>,
    val subscriptions: List<UserSubscription>,
) {
    data class UpgradedGuild(
        val subscriptionId: String,
        val guildId: String,
    )

    /**
     * @param quantities Amount of guilds that the subscription can upgrade
     * @param used Amount of guilds that are currently upgraded with the subscription
     */
    data class UserSubscription(
        val subscriptionId: String,
        val annual: Boolean,
        val quantities: Int,
        val used: Int,
        val available: Int,
    )
}
