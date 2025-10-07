package space.astro.shared.core.daos

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.database.UserData
import space.astro.shared.core.models.redis.RedisHashCacheDao
import space.astro.shared.core.models.redis.RedisKey

@Repository
class UserDao(
    mongoDatabase: MongoDatabase,
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private final var collection: MongoCollection<UserData>

    private val cacheManager = RedisHashCacheDao(
        keyBase = RedisKey.USER_DATA.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    init {
        collection = mongoDatabase.getCollection("users", UserData::class.java)
        collection.createIndex(Indexes.ascending(UserData::userID.name))
    }

    fun get(id: String): UserData? {
        return cacheManager.get(id)
            ?: collection.find(Filters.eq(UserData::userID.name, id))
                .limit(1)
                .firstOrNull()
                ?.also {
                    cacheManager.cache(id, it)
                }
    }

    fun getOrCreate(id: String): UserData {
        return get(id)
            ?: UserData(userID = id).also { save(it) }
    }

    fun save(userData: UserData) {
        collection.replaceOne(
            Filters.eq(UserData::userID.name, userData.userID),
            userData,
            ReplaceOptions().upsert(true)
        )

        cacheManager.cache(userData.userID, userData)
    }
}