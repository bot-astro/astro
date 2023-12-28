package space.astro.api.central.services

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import org.springframework.stereotype.Service
import space.astro.api.central.daos.AuthedUsersDao
import space.astro.api.central.models.DiscordAuthedUser
import space.astro.api.central.models.TokenPayloadDto
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.redis.RedisKey

@Service
class DiscordUserTokenPersistenceService(
    val authedUsersDao: AuthedUsersDao,
    val redis: RedisClusterAsyncCommands<String, String>,
    val dataSerializer: DataSerializer
) {

    suspend fun updateCredentials(userId: Long, tokenPayloadDto: TokenPayloadDto) {
        val id = userId.toString()

        val authedUser = DiscordAuthedUser(id, tokenPayloadDto)
        authedUsersDao.upsertAuthedUser(authedUser)

        val serializedClientCredentials = dataSerializer.serializeData(tokenPayloadDto)
        redis.setex(
            String.format(
                RedisKey.DISCORD_USER_CREDENTIALS.key,
                userId
            ),
            (tokenPayloadDto.expiresIn - 600L),
            serializedClientCredentials
        )
    }
}