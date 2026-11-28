package space.astro.api.central.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.shared.core.utils.redis.RedisKey
import java.security.SecureRandom
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

data class OAuthState(
    val redirectPath: String,
    val source: String,
    /** Set when the login was started by an MCP client authorization request */
    val mcpRequestId: String?,
)

@Service
class OAuthStateService(
    private val redis: StringRedisTemplate,
) {
    private val secureRandom = SecureRandom()

    /**
     * Creates a state string to be used to verify the OAuth flow
     *
     * @param redirectPath the path to redirect to after the flow is complete
     * @param source where the invite came from, must not contain `:`
     * @param mcpRequestId id of the MCP authorization request to continue after the login
     */
    fun create(
        redirectPath: String,
        source: String,
        mcpRequestId: String? = null,
    ): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)

        val state =
            Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)

        redis.opsForValue().set(
            RedisKey.OAUTH_STATE(state),
            "$source:$redirectPath",
            10.minutes.toJavaDuration(),
        )

        if (mcpRequestId != null) {
            redis.opsForValue().set(RedisKey.OAUTH_STATE_MCP_REQUEST(state), mcpRequestId, 10.minutes.toJavaDuration())
        }

        return state
    }

    /**
     * Gets the data related to a state string and
     * deletes the state afterward
     *
     * @return the redirect path and source or null if the state is invalid
     */
    fun consume(state: String): OAuthState? {
        val value = redis.opsForValue().getAndDelete(RedisKey.OAUTH_STATE(state)) ?: return null

        return OAuthState(
            redirectPath = value.substringAfter(':'),
            source = value.substringBefore(':'),
            mcpRequestId = redis.opsForValue().getAndDelete(RedisKey.OAUTH_STATE_MCP_REQUEST(state)),
        )
    }
}
