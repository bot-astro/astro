package space.astro.api.central.services.dashboard

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import space.astro.api.central.properties.CentralApiProperties
import space.astro.api.central.models.auth.AuthSession
import space.astro.shared.core.exceptions.AErrorCode
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class AuthSessionService(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
    private val centralApiProperties: CentralApiProperties,
) {

    companion object {
        val SESSION_TTL = Duration.ofDays(60)
        val SESSION_REFRESH_THRESHOLD = Duration.ofDays(30)
    }

    private val secureRandom = SecureRandom()

    fun createSession(userId: String): AuthSession {
        val sessionId = createSessionId()
        val authSession = AuthSession(
            sessionId = sessionId,
            userId = userId,
            expiresAt = Instant.now().plus(SESSION_TTL).toEpochMilli()
        )
        redis.opsForValue().set(
            RedisKey.AUTH_SESSION(userId, sessionId),
            jsonMapper.writeValueAsString(authSession),
            SESSION_TTL
        )
        return authSession
    }

    fun getSession(userId: String, sessionId: String): AuthSession? {
        return redis.opsForValue().get(RedisKey.AUTH_SESSION(userId, sessionId))
            ?.let {
                jsonMapper.readValue(it)
            }
    }

    fun deleteSession(userId: String, sessionId: String) {
        redis.delete(RedisKey.AUTH_SESSION(userId, sessionId))
    }

    fun createSessionCookie(authSession: AuthSession): ResponseCookie {
        return ResponseCookie.from(
            centralApiProperties.sessionCookieName,
            "${authSession.userId}:${authSession.sessionId}"
        )
            .maxAge(AuthSessionService.SESSION_TTL)
            .httpOnly(true)
            .secure(centralApiProperties.sessionCookieSecure)
            .sameSite(centralApiProperties.sessionCookieSameSite)
            .domain(centralApiProperties.sessionCookieDomain)
            .build()
    }

    /**
     * @return Pair(userId, sessionId)
     */
    fun getUserAndSessionIdFromCookie(cookieValue: String): Pair<String, String> {
        val parts = cookieValue.split(":")
        if (parts.size != 2) {
            throw AException(
                httpStatusCode = 401,
                errorCode = AErrorCode.INVALID_REQUEST,
                message = "Invalid session cookie format"
            )
        }
        return Pair(parts[0], parts[1])
    }

    private fun createSessionId(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)

        return Base64.getEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }
}