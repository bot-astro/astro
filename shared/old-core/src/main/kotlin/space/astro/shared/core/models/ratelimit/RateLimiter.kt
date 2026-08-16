package space.astro.shared.core.models.ratelimit

import java.time.Duration
import java.time.Instant

interface RateLimiter {

    @Throws(InterruptedException::class)
    fun acquire(key: String) {
        while (true) {
            val result = tryAcquire(key)
            if (result.acquired) {
                return
            } else {
                Thread.sleep(result.ttl)
            }
        }
    }

    fun tryAcquire(
        key: String,
        maxPerInterval: Int,
        timeout: Duration
    ): RateLimiterAcquireResult? {
        val endTime = Instant.now().plus(timeout)
        var lastResult: RateLimiterAcquireResult
        do {
            lastResult = tryAcquire(key, maxPerInterval)
            if (lastResult.acquired) {
                return lastResult
            } else {
                if (Instant.now().plusMillis(lastResult.ttl).isAfter(endTime)) {
                    return lastResult
                } else {
                    try {
                        Thread.sleep(lastResult.ttl)
                    } catch (e: InterruptedException) {
                        throw RuntimeException(e)
                    }
                }
            }
        } while (Instant.now().isBefore(endTime))
        return lastResult
    }

    fun tryAcquire(id: String): RateLimiterAcquireResult

    fun tryAcquire(id: String, maxPerInterval: Int): RateLimiterAcquireResult

}
