package space.astro.bot.components.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger { }

/**
 * Temporary VCs currently stored in Redis, across all the guilds (every pod reports the same value, so use max() on it).
 * Refreshed on a schedule instead of on every export because counting walks all the keys.
 */
@Component
class TemporaryVCsGauge(
    private val registry: MeterRegistry,
    private val temporaryVCRepository: TemporaryVCRepository,
) {
    private val active = AtomicLong()

    @PostConstruct
    fun register() {
        Gauge
            .builder("astro.vc.active", active) { it.get().toDouble() }
            .register(registry)
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT5M")
    fun refresh() {
        try {
            active.set(temporaryVCRepository.countAll())
        } catch (e: Exception) {
            log.error(e) { "Failed counting the active temporary VCs" }
        }
    }
}
