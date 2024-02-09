package space.astro.shared.core.daos

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.redis.RedisDynamicHashCacheDao
import space.astro.shared.core.models.redis.RedisHashCacheDao
import space.astro.shared.core.models.redis.RedisKey

@Repository
class GuildDao(
    mongoDatabase: MongoDatabase,
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private final var collection: MongoCollection<GuildData>

    private val cacheManager = RedisHashCacheDao(
        keyBase = RedisKey.GUILD_DATA.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    init {
        collection = mongoDatabase.getCollection("guilds", GuildData::class.java)
        collection.createIndex(Indexes.ascending(GuildData::guildID.name))
    }

    fun get(id: String): GuildData? {
        return cacheManager.get(id)
            ?: collection.find(eq(GuildData::guildID.name, id))
                .limit(1)
                .firstOrNull()
                ?.also {
                    cacheManager.cache(id, it)
                }
    }

    fun getOrCreate(id: String): GuildData {
        return get(id)
            ?: GuildData(guildID = id).also { save(it) }
    }

    fun save(guildData: GuildData) {
        collection.replaceOne(
            eq(GuildData::guildID.name, guildData.guildID),
            guildData,
            ReplaceOptions().upsert(true)
        )

        cacheManager.cache(guildData.guildID, guildData)
    }
}