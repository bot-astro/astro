package space.astro.bot.components.health.indicators

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import space.astro.bot.components.health.ShardsHealthTracker
import org.springframework.stereotype.Component
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Component("shardsLiveness")
class ShardsAliveIndicator(
    val tracker: ShardsHealthTracker
) : HealthIndicator {

    val jitter = Random.nextInt(-30, 30).seconds

    override fun health(): Health {
        val threshold = 5.minutes + jitter

        if (tracker.anyStuck(threshold)) {
            return Health.down().build()
        }

        return Health.up().build()
    }

}
