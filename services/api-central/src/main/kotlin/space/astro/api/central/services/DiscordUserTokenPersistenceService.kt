package space.astro.api.central.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.shared.core.models.database.DiscordUserTokenEntity
import space.astro.shared.core.models.discord.DiscordTokenDto
import space.astro.shared.core.repositories.DiscordUserTokenRepository
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class DiscordUserTokenPersistenceService(
    private val discordUserTokenRepository: DiscordUserTokenRepository,
    private val redisTemplate: StringRedisTemplate,
    private val jsonMapper: JsonMapper
) {
    /**
     * @return a pair of (isExpired, token entity) or null if not found
     */
    fun get(userId: String): Pair<Boolean, DiscordUserTokenEntity>? {
        val serializedData = redisTemplate.opsForValue().get(RedisKey.DISCORD_USER_TOKEN(userId))

        return if (serializedData != null) {
            Pair(false, jsonMapper.readValue(serializedData))
        } else {
            Pair(true, discordUserTokenRepository.findById(userId).orElse(null))
        }
    }

    fun upsert(userId: String, discordTokenDto: DiscordTokenDto) {
        val tokenEntity = DiscordUserTokenEntity.fromTokenPayload(userId, discordTokenDto)
        discordUserTokenRepository.save(tokenEntity)

        redisTemplate.opsForValue().set(
            RedisKey.DISCORD_USER_TOKEN(userId),
            jsonMapper.writeValueAsString(discordTokenDto),
            (discordTokenDto.expiresIn - 300).seconds.toJavaDuration(),
        )
    }

    fun delete(userId: String) {
        discordUserTokenRepository.deleteById(userId)
        redisTemplate.delete(RedisKey.DISCORD_USER_TOKEN(userId))
    }
}