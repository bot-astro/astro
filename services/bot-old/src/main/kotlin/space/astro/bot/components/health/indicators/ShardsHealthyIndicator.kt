package space.astro.bot.components.health.indicators

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import space.astro.bot.components.health.ShardsHealthTracker
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Component("shardsReadiness")
class ShardsHealthyIndicator(
    val tracker: ShardsHealthTracker
) : ReactiveHealthIndicator {
    override fun health(): Mono<Health> {
        return Mono.fromCallable {
            if (tracker.allHealthy()) Health.up().build()
            else Health.down().build()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofSeconds(10))
            .onErrorReturn(Health.down().build())
    }
}