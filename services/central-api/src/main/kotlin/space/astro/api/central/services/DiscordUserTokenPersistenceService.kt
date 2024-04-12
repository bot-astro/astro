package space.astro.api.central.services

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Service
import space.astro.api.central.daos.AuthedUsersDao
import space.astro.api.central.models.discord.DiscordAuthedUser
import space.astro.api.central.models.discord.TokenPayloadDto
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.redis.RedisKey

@Service
class DiscordUserTokenPersistenceService(
    val authedUsersDao: AuthedUsersDao,
    val redis: RedisClusterAsyncCommands<String, String>,
    val redisSync: RedisClusterCommands<String, String>,
    val dataSerializer: DataSerializer,
    val discordUserTokenFetchService: DiscordUserTokenFetchService
) {

    /**
     * @return a [Pair] with the token payload of the user with the provided [userId] and a [Boolean] that is true if the token is valid or false if it has expired, or null if the user never logged in
     */
    suspend fun getCredentials(userId: String): Pair<TokenPayloadDto, Boolean>? {
        val serializedCredentials = redisSync.get(String.format(RedisKey.DISCORD_USER_CREDENTIALS.key, userId))

        return if (serializedCredentials != null) {
            Pair(dataSerializer.deserialize<TokenPayloadDto>(serializedCredentials), true)
        } else {
            val credentials = authedUsersDao.getAuthedUser(userId)?.discordAuthTokenInfo ?: return null
            Pair(credentials, false)
        }
    }

    suspend fun updateCredentials(userId: String, tokenPayloadDto: TokenPayloadDto) {
        val authedUser = DiscordAuthedUser(userId, tokenPayloadDto)
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

    suspend fun deleteCredentials(userId: String) {
        authedUsersDao.deleteAuthedUser(userId)
        redis.del(String.format(RedisKey.DISCORD_USER_CREDENTIALS.key, userId))
    }
}