package space.astro.shared.core.models.database

data class UserData(
    val userID: String,
    val entitlements: MutableList<UserEntitlement> = mutableListOf(),
    var votes: Int = 0,
    var coins: Double = 0.0,
    val settings: UserSettingsDto = UserSettingsDto(),
    val createdFirstVC: Boolean = false,
    var premium: Boolean = false,
    val guildActiveUpgrades: MutableList<GuildUpgradeDto> = mutableListOf(),
)

data class UserEntitlement(
    val id: String,
    val skuId: String,
    val endsAt: Long?
) {
    fun isActive(currentMillis: Long) = (endsAt == null) || (endsAt >= currentMillis)
}

data class UserSettingsDto(
    var interfaceReplies: Boolean = true,
    var interfaceRepliesDeleteAfter: Long? = null
)

data class GuildUpgradeDto(
    val guildID: String,
    val subscriptionID: String,
    val yearly: Boolean
)