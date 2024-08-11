package space.astro.shared.core.daos

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import org.litote.kmongo.descending
import org.springframework.stereotype.Repository
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.influx.ConfigurationErrorData

@Repository
class ConfigurationErrorDao(
    mongoDatabase: MongoDatabase,
) {
    private final var collection: MongoCollection<ConfigurationErrorData>

    init {
        collection = mongoDatabase.getCollection("guilds", ConfigurationErrorData::class.java)
        collection.createIndex(Indexes.ascending(ConfigurationErrorData::guildId.name), IndexOptions().unique(false))
    }

    fun getOfLastSevenDays(guildID: String): List<ConfigurationErrorData> {
        val minTimestamp = System.currentTimeMillis() - 604800000

        return collection.find(and(eq(ConfigurationErrorData::guildId.name, guildID), gte(ConfigurationErrorData::timestamp.name, minTimestamp)))
            .limit(100)
            .sort(descending(ConfigurationErrorData::timestamp))
            .toList()
    }

    fun save(configurationErrorData: ConfigurationErrorData) {
        collection.replaceOne(
            eq(GuildData::guildID.name, configurationErrorData.guildId),
            configurationErrorData,
            ReplaceOptions().upsert(true)
        )
    }

    fun clear(guildId: String) {
        collection.deleteMany(eq(ConfigurationErrorData::guildId.name, guildId))
    }
}