package space.astro.shared.core.dao

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReturnDocument
import org.springframework.stereotype.Component
import space.astro.shared.core.models.database.GuildDto

@Component
class GuildDao(
    mongoDatabase: MongoDatabase
) {
    lateinit var collection: MongoCollection<GuildDto>

    init {
        collection = mongoDatabase.getCollection("guilds", GuildDto::class.java)
        collection.createIndex(Indexes.ascending(GuildDto::guildID.name))
    }
}