package space.astro.bot.components.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import space.astro.bot.properties.PodProperties
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import java.util.concurrent.atomic.AtomicLong

private val log = KotlinLogging.logger { }

/**
 * Temporary VCs currently stored in Redis, across all the guilds.
 * Only pod 0 reports it because the count is global, and it is refreshed on a schedule because counting walks all the keys.
 */
@Component
class TemporaryVCsGauge(
    private val registry: MeterRegistry,
    private val temporaryVCRepository: TemporaryVCRepository,
    podProperties: PodProperties,
) {
    private val enabled = podProperties.getParsedOrdinal() == 0
    private val active = AtomicLong()

    @PostConstruct
    fun register() {
        if (!enabled) return

        Gauge
            .builder("astro.vc.active", active) { it.get().toDouble() }
            .register(registry)
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT5M")
    fun refresh() {
        if (!enabled) return

        try {
            active.set(temporaryVCRepository.countAll())
        } catch (e: Exception) {
            log.error(e) { "Failed counting the active temporary VCs" }
        }
    }
}
