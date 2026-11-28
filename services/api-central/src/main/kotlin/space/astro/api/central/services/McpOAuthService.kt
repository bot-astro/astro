package space.astro.api.central.services

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.api.central.models.mcp.McpAuthCode
import space.astro.api.central.models.mcp.McpAuthRequest
import space.astro.api.central.models.mcp.McpGrant
import space.astro.api.central.models.mcp.McpToken
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

data class McpTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenTtl: Duration,
    val scopes: List<String>,
)

data class McpGrantUsage(
    val grant: McpGrant,
    /** epoch millis, null if no token of the grant was used yet */
    val lastUsedAt: Long?,
)

data class McpTokenIntrospection(
    val token: McpToken,
    val expiresInSeconds: Long,
)

/**
 * State of the OAuth authorization server used by MCP clients:
 * authorization requests, authorization codes and access / refresh tokens.
 *
 * Codes and tokens are random and only their SHA-256 is stored in Redis.
 *
 * Every authorization creates a grant, the tokens obtained by refreshing belong to the same grant.
 * Deleting the grant stops all of them, this is what happens when a refresh token or a code is used twice,
 * when the user disconnects the client or when the client revokes its refresh token.
 */
@Service
class McpOAuthService(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
) {
    companion object {
        val AUTH_REQUEST_TTL: Duration = Duration.ofMinutes(10)
        val AUTH_CODE_TTL: Duration = Duration.ofMinutes(1)
        val AUTH_CODE_USED_TTL: Duration = Duration.ofMinutes(10)
        val ACCESS_TOKEN_TTL: Duration = Duration.ofHours(1)
        val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(30)
    }

    private val secureRandom = SecureRandom()

    fun createAuthRequest(request: McpAuthRequest): String {
        val id = randomString()
        redis.opsForValue().set(RedisKey.MCP_AUTH_REQUEST(id), jsonMapper.writeValueAsString(request), AUTH_REQUEST_TTL)
        return id
    }

    fun getAuthRequest(id: String): McpAuthRequest? =
        redis.opsForValue().get(RedisKey.MCP_AUTH_REQUEST(id))?.let { jsonMapper.readValue(it) }

    fun setAuthRequestUser(
        id: String,
        userId: String,
    ) {
        val request = getAuthRequest(id) ?: return
        redis.opsForValue().set(
            RedisKey.MCP_AUTH_REQUEST(id),
            jsonMapper.writeValueAsString(request.copy(userId = userId)),
            AUTH_REQUEST_TTL,
        )
    }

    /**
     * Ends an authorization request, a request can be approved or denied only once
     *
     * @param grantedScopes the scopes the user accepted, a subset of the requested ones
     * @return the code to give to the client, or null if the request is not valid or the user is not logged in
     */
    fun approveAuthRequest(
        id: String,
        grantedScopes: List<String>,
    ): Pair<McpAuthRequest, String>? {
        val request = consumeAuthRequest(id) ?: return null
        val userId = request.userId ?: return null

        val code = randomString()
        redis.opsForValue().set(
            RedisKey.MCP_AUTH_CODE(sha256(code)),
            jsonMapper.writeValueAsString(
                McpAuthCode(
                    request.clientId,
                    request.clientName,
                    request.redirectUri,
                    request.codeChallenge,
                    grantedScopes,
                    request.resource,
                    userId,
                ),
            ),
            AUTH_CODE_TTL,
        )
        return Pair(request, code)
    }

    fun denyAuthRequest(id: String): McpAuthRequest? = consumeAuthRequest(id)

    /**
     * @param resource the `resource` of the token request, if the client sent it
     * A code that was already exchanged revokes the tokens issued from it (RFC 6749 4.1.2).
     *
     * @return the tokens, or null if the code is invalid or was issued for another client, redirect URI or resource,
     * or the PKCE verifier does not match
     */
    fun exchangeCode(
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String,
        resource: String?,
    ): McpTokenPair? {
        val hash = sha256(code)
        // the code is deleted even if the checks below fail so it can't be guessed by retrying
        val authCode: McpAuthCode? =
            redis.opsForValue().getAndDelete(RedisKey.MCP_AUTH_CODE(hash))?.let { jsonMapper.readValue(it) }

        if (authCode == null) {
            redis.opsForValue().get(RedisKey.MCP_AUTH_CODE_USED(hash))?.let { revokeGrant(it) }
            return null
        }

        val challenge =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray()),
            )

        val sameRequest = authCode.clientId == clientId && authCode.redirectUri == redirectUri
        val sameResource = resource == null || resource == authCode.resource
        if (!sameRequest || !sameResource || !MessageDigest.isEqual(challenge.toByteArray(), authCode.codeChallenge.toByteArray())) {
            return null
        }

        val grant =
            McpGrant(
                grantId = randomString(),
                userId = authCode.userId,
                clientId = clientId,
                clientName = authCode.clientName,
                scopes = authCode.scopes,
                createdAt = System.currentTimeMillis(),
            )
        redis.opsForValue().set(RedisKey.MCP_GRANT(grant.grantId), jsonMapper.writeValueAsString(grant), REFRESH_TOKEN_TTL)
        redis.opsForSet().add(RedisKey.MCP_USER_GRANTS(grant.userId), grant.grantId)
        redis.opsForValue().set(RedisKey.MCP_AUTH_CODE_USED(hash), grant.grantId, AUTH_CODE_USED_TTL)

        return issueTokens(grant.grantId, authCode.userId, clientId, authCode.scopes, authCode.resource)
    }

    /**
     * Rotates the refresh token, the used one stops working.
     * Using a refresh token that was already rotated means it leaked, so the whole grant is revoked.
     *
     * @return the new tokens or null if the refresh token is invalid, revoked or belongs to another client
     */
    fun refresh(
        refreshToken: String,
        clientId: String,
    ): McpTokenPair? {
        val hash = sha256(refreshToken)
        val token: McpToken? =
            redis.opsForValue().getAndDelete(RedisKey.MCP_REFRESH_TOKEN(hash))?.let { jsonMapper.readValue(it) }

        if (token == null) {
            redis.opsForValue().get(RedisKey.MCP_REFRESH_TOKEN_USED(hash))?.let { revokeGrant(it) }
            return null
        }

        if (token.clientId != clientId || !isGrantActive(token.grantId)) return null

        redis.opsForValue().set(RedisKey.MCP_REFRESH_TOKEN_USED(hash), token.grantId, REFRESH_TOKEN_TTL)

        return issueTokens(token.grantId, token.userId, clientId, token.scopes, token.resource)
    }

    fun getAccessToken(accessToken: String): McpToken? {
        val token =
            redis
                .opsForValue()
                .get(RedisKey.MCP_ACCESS_TOKEN(sha256(accessToken)))
                ?.let<String, McpToken> { jsonMapper.readValue(it) }
                ?.takeIf { isGrantActive(it.grantId) }
                ?: return null
        redis.opsForValue().set(
            RedisKey.MCP_GRANT_LAST_USED(token.grantId),
            System.currentTimeMillis().toString(),
            REFRESH_TOKEN_TTL,
        )
        return token
    }

    /**
     * @return the access token and the seconds it is still valid for, or null if it is not valid
     */
    fun introspect(accessToken: String): McpTokenIntrospection? {
        val token = getAccessToken(accessToken) ?: return null
        val expiresIn = redis.getExpire(RedisKey.MCP_ACCESS_TOKEN(sha256(accessToken)), TimeUnit.SECONDS)
        if (expiresIn <= 0) return null
        return McpTokenIntrospection(token, expiresIn)
    }

    fun hasActiveGrants(userId: String): Boolean {
        val key = RedisKey.MCP_USER_GRANTS(userId)
        val grantIds = redis.opsForSet().members(key).orEmpty()
        val (active, ended) = grantIds.partition { isGrantActive(it) }
        if (ended.isNotEmpty()) redis.opsForSet().remove(key, *ended.toTypedArray())
        return active.isNotEmpty()
    }

    /**
     * @return the active grants of the user, oldest first
     */
    fun listGrants(userId: String): List<McpGrantUsage> {
        val key = RedisKey.MCP_USER_GRANTS(userId)
        val grantIds = redis.opsForSet().members(key).orEmpty()
        val grants = grantIds.mapNotNull { getGrant(it) }
        val ended = grantIds - grants.map { it.grantId }.toSet()
        if (ended.isNotEmpty()) redis.opsForSet().remove(key, *ended.toTypedArray())

        return grants
            .sortedBy { it.createdAt }
            .map { McpGrantUsage(it, redis.opsForValue().get(RedisKey.MCP_GRANT_LAST_USED(it.grantId))?.toLongOrNull()) }
    }

    /**
     * Revokes every token of the grant
     *
     * @return false if the user has no active grant with this id
     */
    fun revokeUserGrant(
        userId: String,
        grantId: String,
    ): Boolean {
        if (getGrant(grantId)?.userId != userId) return false
        revokeGrant(grantId)
        return true
    }

    /**
     * Token revocation (RFC 7009): a refresh token revokes its whole grant, an access token only itself.
     * Unknown tokens are ignored.
     */
    fun revokeToken(token: String) {
        val hash = sha256(token)
        val refreshToken: McpToken? = redis.opsForValue().get(RedisKey.MCP_REFRESH_TOKEN(hash))?.let { jsonMapper.readValue(it) }
        if (refreshToken != null) {
            revokeGrant(refreshToken.grantId)
            return
        }
        redis.delete(RedisKey.MCP_ACCESS_TOKEN(hash))
    }

    private fun getGrant(grantId: String): McpGrant? =
        redis.opsForValue().get(RedisKey.MCP_GRANT(grantId))?.let { jsonMapper.readValue(it) }

    private fun isGrantActive(grantId: String) = redis.hasKey(RedisKey.MCP_GRANT(grantId))

    private fun revokeGrant(grantId: String) {
        val grant = getGrant(grantId) ?: return
        redis.delete(listOf(RedisKey.MCP_GRANT(grantId), RedisKey.MCP_GRANT_LAST_USED(grantId)))
        redis.opsForSet().remove(RedisKey.MCP_USER_GRANTS(grant.userId), grantId)
    }

    private fun issueTokens(
        grantId: String,
        userId: String,
        clientId: String,
        scopes: List<String>,
        resource: String,
    ): McpTokenPair {
        val token =
            McpToken(
                tokenId = randomString(),
                grantId = grantId,
                userId = userId,
                clientId = clientId,
                scopes = scopes,
                resource = resource,
            )
        // the grant lives as long as its newest refresh token
        redis.expire(RedisKey.MCP_GRANT(grantId), REFRESH_TOKEN_TTL)
        val serializedToken = jsonMapper.writeValueAsString(token)
        val accessToken = randomString()
        val refreshToken = randomString()

        redis.opsForValue().set(RedisKey.MCP_ACCESS_TOKEN(sha256(accessToken)), serializedToken, ACCESS_TOKEN_TTL)
        redis.opsForValue().set(RedisKey.MCP_REFRESH_TOKEN(sha256(refreshToken)), serializedToken, REFRESH_TOKEN_TTL)

        return McpTokenPair(accessToken, refreshToken, ACCESS_TOKEN_TTL, scopes)
    }

    private fun consumeAuthRequest(id: String): McpAuthRequest? =
        redis.opsForValue().getAndDelete(RedisKey.MCP_AUTH_REQUEST(id))?.let { jsonMapper.readValue(it) }

    private fun randomString(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(value: String): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))
}
