package space.astro.api.central.services

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.springframework.stereotype.Service
import space.astro.shared.core.utils.redis.RedisKey

@Service
class RateLimitingService(
    private val proxyManager: ProxyManager<String>,
    private val bucketConfiguration: BucketConfiguration,
) {
    fun resolveBucket(clientId: String): Bucket {
        return proxyManager.getProxy(RedisKey.RATE_LIMIT("api", clientId)) {
            bucketConfiguration
        }
    }
}