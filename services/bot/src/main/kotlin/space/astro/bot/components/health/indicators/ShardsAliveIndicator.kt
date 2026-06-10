package space.astro.bot.components.health.indicators

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import space.astro.bot.components.health.ShardsHealthTracker
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import java.time.Duration

@Component("shardsLiveness")
class ShardsAliveIndicator(
    val tracker: ShardsHealthTracker
) : ReactiveHealthIndicator {
    val jitter = Random.nextInt(-30, 30).seconds

    override fun health(): Mono<Health> {
        val threshold = 5.minutes + jitter
        return Mono.fromCallable {
            if (tracker.anyStuck(threshold)) Health.down().build()
            else Health.up().build()
        }
            .subscribeOn(Schedulers.boundedElastic())
            .timeout(Duration.ofSeconds(10))
            .onErrorReturn(Health.down().build())
    }
}