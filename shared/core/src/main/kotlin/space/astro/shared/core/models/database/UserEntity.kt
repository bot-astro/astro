package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.astro.shared.core.models.database.userSettings.UserGuildUpgradeData

@Document(collection = "users")
data class UserEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed(unique = true)
    val userID: String,
    val guildActiveUpgrades: MutableList<UserGuildUpgradeData> = mutableListOf(),
)
