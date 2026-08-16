package space.astro.shared.core.models.ratelimit

data class RateLimiterAcquireResult(
    val acquired: Boolean,
    val count: Long = 0,
    val ttl: Long = 0,
)
    