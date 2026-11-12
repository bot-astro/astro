package space.astro.api.central.services

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.springframework.stereotype.Service
import space.astro.shared.core.utils.redis.RedisKey
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Service
class RateLimitingService(
    private val proxyManager: ProxyManager<String>,
) {
    companion object {
        const val BUCKET_API_GLOBAL_CAPACITY: Long = 100
        const val BUCKET_MEDIA_CAPACITY: Long = 40
    }

    /**
     * @return the rate limit [Bucket] for generic api requests
     */
    fun resolveApiGlobalBucket(clientId: String): Bucket =
        proxyManager.getProxy(RedisKey.RATE_LIMIT("api", clientId)) {
            BucketConfiguration
                .builder()
                .addLimit { l ->
                    l
                        .capacity(BUCKET_API_GLOBAL_CAPACITY)
                        .refillGreedy(BUCKET_API_GLOBAL_CAPACITY, 1.minutes.toJavaDuration())
                }.build()
        }

    /**
     * @return the rate limit [Bucket] for media uploads
     */
    fun resolveMediaBucket(guildId: String): Bucket =
        proxyManager.getProxy(RedisKey.RATE_LIMIT("api_media", guildId)) {
            BucketConfiguration
                .builder()
                .addLimit { l ->
                    l
                        .capacity(BUCKET_MEDIA_CAPACITY)
                        .refillGreedy(BUCKET_MEDIA_CAPACITY, 1.days.toJavaDuration())
                }.build()
        }
}
