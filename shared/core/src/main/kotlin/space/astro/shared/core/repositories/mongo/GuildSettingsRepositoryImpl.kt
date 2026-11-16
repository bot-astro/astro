package space.astro.shared.core.repositories.mongo

import org.bson.Document
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
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
 * This is done with a Mongo event because Spring Data does not let a custom implementation override `save`.
 *
 * Implements the custom methods of [GuildSettingsRepository], Spring Data finds it by its name.
 */
class GuildSettingsRepositoryImpl(
    private val mongoOperations: MongoOperations,
    private val redis: StringRedisTemplate,
) : AbstractMongoEventListener<GuildSettingsEntity>() {
    private companion object {
        val CACHE_TTL: Duration = Duration.ofDays(7)
    }

    fun findByGuildID(guildID: String): GuildSettingsEntity? {
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

    override fun onAfterSave(event: AfterSaveEvent<GuildSettingsEntity>) {
        redis.delete(RedisKey.GUILD_SETTINGS(event.source.guildID))
    }
}
