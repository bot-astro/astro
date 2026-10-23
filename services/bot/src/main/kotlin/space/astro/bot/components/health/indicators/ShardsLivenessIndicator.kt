package space.astro.bot.components.health.indicators

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component
import space.astro.bot.components.health.ShardsHealthTracker
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Component("shardsLiveness")
class ShardsLivenessIndicator(
    private val tracker: ShardsHealthTracker,
) : HealthIndicator {
    private val jitter = Random.nextInt(-30, 30).seconds

    override fun health(): Health =
        try {
            if (tracker.anyStuck(5.minutes + jitter)) Health.down().build() else Health.up().build()
        } catch (_: Exception) {
            Health.down().build()
        }
}
