package space.astro.shared.core.services.dao

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoDatabase
import org.springframework.stereotype.Repository
import space.astro.shared.core.models.database.GuildData

// TODO: Redis cache on top?
@Repository
class GuildDao(
    mongoDatabase: MongoDatabase
) {
    private final var collection: MongoCollection<GuildData>

    init {
        collection = mongoDatabase.getCollection("guilds", GuildData::class.java)
        collection.createIndex(Indexes.ascending(GuildData::guildID.name))
    }

    fun get(id: String): GuildData? {
        return collection.find(eq(GuildData::guildID.name, id)).firstOrNull()
    }

    fun getOrCreate(id: String): GuildData {
        return get(id)
            ?: GuildData(guildID = id).also { save(it) }
    }

    fun save(guildData: GuildData) {
        collection.replaceOne(
            filter = eq(GuildData::guildID.name, guildData.guildID),
            replacement = guildData,
            options = ReplaceOptions().upsert(true)
        )
    }
}