package space.astro.api.central.services.dashboard

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.impl.DefaultJwtBuilder
import io.jsonwebtoken.security.Keys
import io.lettuce.core.GetExArgs
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import org.springframework.stereotype.Service
import space.astro.api.central.configs.JwtConfig
import space.astro.shared.core.models.redis.RedisKey
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * Service to store user web sessions for the frontend dashboard
 */
@Service
class WebSessionService(
    jwtConfig: JwtConfig,
    val redis: RedisClusterAsyncCommands<String, String>
) {

    companion object {
        private val SESSION_TTL = Duration.ofDays(7).toSeconds()
    }

    val sessionCache: Cache<String, Boolean> =
        Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build()

    val parser: JwtParser = Jwts.parserBuilder().setSigningKey(jwtConfig.getDecodedKey()).build()

    val hmacShaKey: SecretKey = Keys.hmacShaKeyFor(jwtConfig.getDecodedKey())


    /**
     * @param userID
     * @return the session token
     */
    fun createSession(userID: String): String {
        val token = createToken(userID)
        cacheToken(userID, token)
        return token
    }

    /**
     * Parses the given [token] and returns the user ID of the session
     *
     * @param token
     *
     * @return the user ID attached to the token
     */
    fun getUserIdFromSession(token: String): String? {
        val claims = parser.parseClaimsJws(token)
        val id = claims.body.id

        redis.getex(buildRedisKey(id, token), GetExArgs().ex(SESSION_TTL)) ?: return null

        return id
    }

    /**
     * Deletes all sessions of the given [userID]
     *
     * @param userID
     */
    fun deleteSessions(userID: String) {
        val key = buildRedisKeyAllSessions(userID)

        val tokens = redis.keys(key)
            .get()

        sessionCache.invalidateAll(tokens)
        redis.del(*tokens.toTypedArray())
    }


    private fun cacheToken(userID: String, token: String) {
        val key = buildRedisKey(userID, token)
        val value = true

        sessionCache.put(key, value)
        redis.setex(
            key, SESSION_TTL, value.toString()
        )
    }

    private fun createToken(userID: String): String {
        return DefaultJwtBuilder()
            .setIssuer("astro-web")
            .setIssuedAt(Date.from(Instant.now()))
            .setId(userID)
            .signWith(hmacShaKey)
            .compact()
    }

    private fun buildRedisKey(id: String, token: String): String {
        return String.format(RedisKey.WEB_SESSION_TOKEN.key, id, token)
    }

    private fun buildRedisKeyAllSessions(userID: String): String {
        return String.format(RedisKey.WEB_SESSION_TOKENS.key, userID)
    }
}