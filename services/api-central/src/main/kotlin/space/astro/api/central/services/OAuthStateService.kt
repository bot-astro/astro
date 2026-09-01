package space.astro.api.central.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.shared.core.utils.redis.RedisKey
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Service
class OAuthStateService(
    private val redis: StringRedisTemplate
) {
    private val secureRandom = SecureRandom()

    /**
     * Creates a state string to be used to verify the OAuth flow
     *
     * @param redirectPath the path to redirect to after the flow is complete
     */
    fun create(redirectPath: String): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)

        val state = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)

        redis.opsForValue().set(
            RedisKey.OAUTH_STATE(state),
            redirectPath,
            10.minutes.toJavaDuration()
        )

        return state
    }

    /**
     * Gets the redirect path related to a state string and
     * deletes the state afterward
     *
     * @return the redirect direct path or null if the state is invalid
     */
    fun consume(state: String): String? =
        redis.opsForValue().getAndDelete(RedisKey.OAUTH_STATE(state))
}