package space.astro.shared.core.models.database.userSettings

data class UserGuildUpgradeData(
    val guildID: String,
    val subscriptionID: String,
    val yearly: Boolean
)