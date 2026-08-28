package space.astro.api.central.services

import net.dv8tion.jda.api.Permission
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.api.central.models.responses.DiscordUserGuild
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.utils.redis.RedisDynamicHashCacheRepository
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper

@Service
class DiscordUserGuildsPersistenceService(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
    private val discordApiClient: DiscordApiClient,
) {
    private val cacheManager = RedisDynamicHashCacheRepository(
        keyBase = RedisKey.DISCORD_USER_GUILDS(),
        redis = redis,
        jsonMapper = jsonMapper
    )

    fun getUserGuilds(userId: String): List<DiscordUserGuild> {
        return cacheManager.getAll(userId)
    }

    fun getUserGuild(userId: String, guildId: String): DiscordUserGuild? {
        return cacheManager.get(userId, guildId)
    }

    /**
     * @throws space.astro.shared.core.exceptions.AException
     */
    fun fetchFromDiscord(userId: String, userDiscordAccessToken: String): List<DiscordUserGuild> {
        val guilds = discordApiClient.getGuilds(userDiscordAccessToken)
            .map { partialGuildDto ->
                DiscordUserGuild(
                    id = partialGuildDto.id,
                    name = partialGuildDto.name,
                    icon = partialGuildDto.icon,
                    permissions = partialGuildDto.permissions,
                    canManage = Permission.getPermissions(partialGuildDto.permissions).any {
                        it == Permission.MANAGE_CHANNEL || it == Permission.MANAGE_SERVER || it == Permission.ADMINISTRATOR
                    }
                )
            }

        updateUserGuilds(userId, guilds)

        return guilds
    }

    fun updateUserGuilds(userId: String, guilds: List<DiscordUserGuild>) {
        cacheManager.cacheAll(userId, guilds.associateBy { it.id })
    }

    fun deleteUserGuilds(userId: String) {
        cacheManager.deleteAll(userId)
    }
}