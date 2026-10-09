package space.astro.api.central.components.security.ratelimit

import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.lettuce.core.api.StatefulRedisConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@Configuration
class RateLimiterConfiguration {
    companion object {
        const val BUCKET_CAPACITY: Long = 100
    }

    @Bean
    fun bucketConfiguration(): BucketConfiguration {
        return BucketConfiguration.builder()
            .addLimit { l -> l
                .capacity(BUCKET_CAPACITY)
                .refillGreedy(100, 1.minutes.toJavaDuration())
            }
            .build()
    }

    @Bean
    fun lettuceProxyManager(
        redisConnection: StatefulRedisConnection<String, ByteArray>
    ): ProxyManager<String> {
        return Bucket4jLettuce.casBasedBuilder(redisConnection)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(
                2.minutes.toJavaDuration()
                    )
            )
            .build()
    }
}