package space.astro.api.central.components.security.ratelimit

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
    @Bean
    fun lettuceProxyManager(redisConnection: StatefulRedisConnection<String, ByteArray>): ProxyManager<String> =
        Bucket4jLettuce
            .casBasedBuilder(redisConnection)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(
                        2.minutes.toJavaDuration(),
                    ),
            ).build()
}
