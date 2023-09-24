package space.astro.shared.core.dao

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoDatabase
import org.springframework.stereotype.Component
import space.astro.shared.core.models.database.GuildDto

@Component
class GuildDao(
    mongoDatabase: MongoDatabase
) {
    private final var collection: MongoCollection<GuildDto>

    init {
        collection = mongoDatabase.getCollection("guilds", GuildDto::class.java)
        collection.createIndex(Indexes.ascending(GuildDto::guildID.name))
    }

    fun get(id: String): GuildDto? {
        return collection.find(eq(GuildDto::guildID.name, id)).firstOrNull()
    }
}