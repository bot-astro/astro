package space.astro.bot.components.health.indicators

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import space.astro.bot.components.health.ShardsHealthTracker
import org.springframework.stereotype.Component

@Component("shardsReadiness")
class ShardsHealthyIndicator(
    val tracker: ShardsHealthTracker
) : HealthIndicator {

    override fun health(): Health {
        if (tracker.allHealthy()) {
            return Health.up().build()
        }

        return Health.down().build()
    }

}
