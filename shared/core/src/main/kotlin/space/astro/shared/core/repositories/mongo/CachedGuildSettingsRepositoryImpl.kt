package space.astro.shared.core.repositories.mongo

import io.github.oshai.kotlinlogging.KotlinLogging
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

private val log = KotlinLogging.logger { }

/**
 * Entities are cached as the JSON of their Mongo document (written and read by the Mongo converter),
 * so the cache can never disagree with how the entity is stored (ObjectId, `@Field` names...).
 *
 * This is done with a Mongo event because Spring Data does not let a fragment override `save`.
 */
class CachedGuildSettingsRepositoryImpl(
    private val mongoOperations: MongoOperations,
    private val redis: StringRedisTemplate,
) : AbstractMongoEventListener<GuildSettingsEntity>(),
    CachedGuildSettingsRepository {
    private companion object {
        val CACHE_TTL: Duration = Duration.ofDays(7)
    }

    override fun findByGuildID(guildID: String): GuildSettingsEntity? {
        val key = RedisKey.GUILD_SETTINGS(guildID)

        redis.opsForValue().get(key)?.let {
            try {
                return mongoOperations.converter.read(GuildSettingsEntity::class.java, Document.parse(it))
            } catch (e: Exception) {
                // e.g. the entity changed since the entry was cached, Mongo is the source of truth and the entry is replaced below
                log.warn(e) { "Unreadable cached guild settings of guild $guildID, reloading them from Mongo" }
            }
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
