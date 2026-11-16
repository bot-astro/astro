package space.astro.shared.core.repositories.mongo

import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.redis.core.StringRedisTemplate
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration

/**
 * Entities are cached as the JSON of their Mongo document (written and read by the Mongo converter),
 * so the cache can never disagree with how the entity is stored (ObjectId, `@Field` names...).
 *
 * Saves invalidate the key instead of rewriting it: bot and api-central both save the whole document,
 * so rewriting could leave an older copy in the cache until the TTL expires.
 */
class CachedGuildSettingsRepositoryImpl(
    private val mongoOperations: MongoOperations,
    private val redis: StringRedisTemplate,
) : CachedGuildSettingsRepository {
    companion object {
        private val CACHE_TTL = Duration.ofDays(7)
    }

    override fun findByGuildID(guildID: String): GuildSettingsEntity? {
        val key = RedisKey.GUILD_SETTINGS(guildID)

        redis.opsForValue().get(key)?.let {
            return mongoOperations.converter.read(GuildSettingsEntity::class.java, Document.parse(it))
        }

        return mongoOperations
            .findOne(
                Query.query(Criteria.where(GuildSettingsEntity::guildID.name).`is`(guildID)),
                GuildSettingsEntity::class.java,
            )?.also {
                val document = Document()
                mongoOperations.converter.write(it, document)
                redis.opsForValue().set(key, document.toJson(), CACHE_TTL)
            }
    }
}
