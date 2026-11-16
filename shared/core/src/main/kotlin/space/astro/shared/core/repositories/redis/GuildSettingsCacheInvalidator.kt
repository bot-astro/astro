package space.astro.shared.core.repositories.redis

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.utils.redis.RedisKey

/**
 * Drops the cached guild settings every time they are saved, no matter who saves them
 */
@Component
class GuildSettingsCacheInvalidator(
    private val redis: StringRedisTemplate,
) : AbstractMongoEventListener<GuildSettingsEntity>() {
    override fun onAfterSave(event: AfterSaveEvent<GuildSettingsEntity>) {
        redis.delete(RedisKey.GUILD_SETTINGS(event.source.guildID))
    }
}
