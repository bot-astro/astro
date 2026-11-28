package space.astro.api.central.components

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingResponseWrapper
import space.astro.api.central.models.auth.AuthPrincipal
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.exceptions.AIdempotencyInProgressException
import space.astro.shared.core.exceptions.AIdempotencyKeyReusedException
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.time.Duration
import java.util.*

data class IdempotencyRecord(
    /** Hash of the request, to detect a key that is reused for a different request */
    val fingerprint: String,
    val completed: Boolean,
    val status: Int = 0,
    val contentType: String? = null,
    val body: String? = null,
)

/**
 * Makes `POST` and `PATCH` requests safe to retry: when a request has an `Idempotency-Key` header the first
 * successful response is stored for 24 hours and sent again for every request that repeats the same key.
 *
 * Keys are scoped to the user. Reusing a key for a different request fails (422), a request that arrives while
 * the first one is still running fails (409, retry after a second). Only successful responses are stored, a failed
 * request releases the key so that it can be retried with the same one.
 */
@Component
class IdempotencyFilter(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
    private val exceptionHandler: ControllersExceptionHandler,
) : OncePerRequestFilter() {
    companion object {
        const val HEADER = "Idempotency-Key"
        const val REPLAYED_HEADER = "Idempotent-Replayed"

        private const val MAX_KEY_LENGTH = 255
        private val IN_PROGRESS_TTL: Duration = Duration.ofMinutes(1)
        private val RESULT_TTL: Duration = Duration.ofHours(24)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.getHeader(HEADER) == null ||
            (request.method != HttpMethod.POST.name() && request.method != HttpMethod.PATCH.name())

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // requests without a session fail later in the chain
        val userId = (SecurityContextHolder.getContext().authentication?.principal as? AuthPrincipal)?.userId
        if (userId == null) {
            filterChain.doFilter(request, response)
            return
        }

        val key = request.getHeader(HEADER)
        if (key.isBlank() || key.length > MAX_KEY_LENGTH) {
            exceptionHandler.writeException(
                response,
                ABadRequestException("$HEADER must be between 1 and $MAX_KEY_LENGTH characters.", param = HEADER),
            )
            return
        }
        if (request.contentType?.startsWith("multipart/", ignoreCase = true) == true) {
            exceptionHandler.writeException(
                response,
                ABadRequestException("$HEADER is not supported on multipart requests.", param = HEADER),
            )
            return
        }

        val body = request.inputStream.readAllBytes()
        val fingerprint =
            sha256("${request.method} ${request.requestURI}?${request.queryString.orEmpty()}\n".toByteArray() + body)
        val redisKey = RedisKey.IDEMPOTENCY(userId, sha256(key.toByteArray()))

        val isFirstRequest =
            redis.opsForValue().setIfAbsent(redisKey, jsonMapper.writeValueAsString(IdempotencyRecord(fingerprint, false)), IN_PROGRESS_TTL)
        if (isFirstRequest != true) {
            val existing: IdempotencyRecord? = redis.opsForValue().get(redisKey)?.let { jsonMapper.readValue(it) }
            when {
                existing != null && existing.fingerprint != fingerprint ->
                    exceptionHandler.writeException(response, AIdempotencyKeyReusedException())

                existing != null && existing.completed -> replay(response, existing)

                else -> exceptionHandler.writeException(response, AIdempotencyInProgressException())
            }
            return
        }

        val cachingResponse = ContentCachingResponseWrapper(response)
        try {
            filterChain.doFilter(ReplayableRequest(request, body), cachingResponse)
        } catch (e: Throwable) {
            redis.delete(redisKey)
            throw e
        }

        if (cachingResponse.status in 200..299) {
            val record =
                IdempotencyRecord(
                    fingerprint = fingerprint,
                    completed = true,
                    status = cachingResponse.status,
                    contentType = cachingResponse.contentType,
                    body = String(cachingResponse.contentAsByteArray, Charsets.UTF_8),
                )
            redis.opsForValue().set(redisKey, jsonMapper.writeValueAsString(record), RESULT_TTL)
        } else {
            redis.delete(redisKey)
        }
        cachingResponse.copyBodyToResponse()
    }

    private fun replay(
        response: HttpServletResponse,
        record: IdempotencyRecord,
    ) {
        response.status = record.status
        record.contentType?.let { response.contentType = it }
        response.setHeader(REPLAYED_HEADER, "true")
        record.body?.let { response.outputStream.write(it.toByteArray(Charsets.UTF_8)) }
    }

    private fun sha256(value: ByteArray): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value))

    /** The body is read to build the fingerprint, this gives it again to the controllers */
    private class ReplayableRequest(
        request: HttpServletRequest,
        private val body: ByteArray,
    ) : HttpServletRequestWrapper(request) {
        override fun getInputStream(): ServletInputStream {
            val stream = ByteArrayInputStream(body)
            return object : ServletInputStream() {
                override fun read(): Int = stream.read()

                override fun isFinished(): Boolean = stream.available() == 0

                override fun isReady(): Boolean = true

                override fun setReadListener(readListener: ReadListener?) = throw UnsupportedOperationException()
            }
        }

        override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(inputStream, characterEncoding ?: "UTF-8"))
    }
}
