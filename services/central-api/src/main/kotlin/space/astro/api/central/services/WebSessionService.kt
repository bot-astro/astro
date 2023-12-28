package space.astro.api.central.services

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

    fun createSession(id: Long): String {
        val token = createToken(id)
        cacheToken(id, token)
        return token
    }

    private fun cacheToken(id: Long, token: String) {
        val key = buildRedisKey(id, token)
        val value = true

        sessionCache.put(key, value)
        redis.setex(
            key, SESSION_TTL, value.toString()
        )
    }

    private fun createToken(id: Long): String {
        return DefaultJwtBuilder().setIssuer("astro-web").setIssuedAt(Date.from(Instant.now()))
            .setId(id.toString()).signWith(hmacShaKey).compact()
    }

    fun existsSession(token: String): Boolean {
        if (sessionCache.getIfPresent(token) != null) {
            return true
        }

        val id = getIdFromSession(token) ?: return false

        return redis.exists(
            buildRedisKey(id, token)
        ).get() == 1L
    }

    fun getIdFromSession(token: String): Long? {
        val claims = parser.parseClaimsJws(token)
        val id = claims.body.id.toLong()

        redis.getex(buildRedisKey(id, token), GetExArgs().ex(SESSION_TTL)) ?: return null

        return id
    }

    fun deleteSessions(id: Long) {
        val key = buildRedisKeyAllSessions(id)

        val tokens = redis.keys(key)
            .get()

        sessionCache.invalidateAll(tokens)
        redis.del(*tokens.toTypedArray())
    }

    private fun buildRedisKey(id: Long, token: String): String {
        return String.format(RedisKey.WEB_SESSION_TOKEN.key, id, token)
    }

    private fun buildRedisKeyAllSessions(id: Long): String {
        return String.format(RedisKey.WEB_SESSION_TOKENS.key, id)
    }
}