package space.astro.api.central.daos

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.springframework.stereotype.Component
import space.astro.api.central.models.DiscordAuthedUser

@Component
class AuthedUsersDao(
    mongoDatabase: MongoDatabase
) {

    lateinit var collection: MongoCollection<DiscordAuthedUser>

    init {
        collection = mongoDatabase.getCollection("authedUsers", DiscordAuthedUser::class.java)
    }

    suspend fun getAuthedUser(userId: String): DiscordAuthedUser? {
        return collection.find(Filters.eq("id", userId)).firstOrNull()
    }

    suspend fun insertAuthedUser(user: DiscordAuthedUser) {
        collection.insertOne(user)
    }

    suspend fun upsertAuthedUser(user: DiscordAuthedUser) {
        collection.replaceOne(Filters.eq("id", user.id), user, ReplaceOptions().upsert(true))
    }

    suspend fun deleteAuthedUser(userId: String) {
        collection.deleteOne(Filters.eq("id", userId))
    }
}