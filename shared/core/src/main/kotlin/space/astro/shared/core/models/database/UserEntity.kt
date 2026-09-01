package space.astro.shared.core.models.database

import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.astro.shared.core.models.database.userSettings.UserEntitlementData
import space.astro.shared.core.models.database.userSettings.UserGuildUpgradeData
import space.astro.shared.core.models.database.userSettings.UserSettingsData

@Document(collection = "users")
data class UserEntity(
    @Indexed(unique = true)
    val userID: String,
//    val entitlements: MutableList<UserEntitlementData> = mutableListOf(),
//    var votes: Int = 0,
//    var coins: Double = 0.0,
//    val settings: UserSettingsData = UserSettingsData(),
//    val createdFirstVC: Boolean = false,
//    var premium: Boolean = false,
//    val guildActiveUpgrades: MutableList<UserGuildUpgradeData> = mutableListOf(),
) {
//    val hasUltimate = entitlements.any { it.isActive(System.currentTimeMillis()) } || guildActiveUpgrades.isNotEmpty()
}


