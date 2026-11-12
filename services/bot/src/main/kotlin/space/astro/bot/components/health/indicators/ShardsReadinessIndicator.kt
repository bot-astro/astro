package space.astro.bot.components.health.indicators

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component
import space.astro.bot.components.health.ShardsHealthTracker

@Component("shardsReadiness")
class ShardsReadinessIndicator(
    private val tracker: ShardsHealthTracker,
) : HealthIndicator {
    override fun health(): Health =
        try {
            if (tracker.allHealthy()) Health.up().build() else Health.down().build()
        } catch (_: Exception) {
            Health.down().build()
        }
}
