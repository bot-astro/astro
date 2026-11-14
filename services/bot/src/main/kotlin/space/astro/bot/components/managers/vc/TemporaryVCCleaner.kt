package space.astro.bot.components.managers.vc

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.lang.L
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

/**
 * Deletes temporary voice channels that are left over (empty, or whose data went missing),
 * for example because a voice event was missed during a restart or a delete failed.
 *
 * Checking is free: it only reads Redis and the JDA cache. Discord requests happen only
 * for actual leftovers, capped per guild and paced to stay far from the rate limits.
 *
 * Premium guilds are swept every [PREMIUM_INTERVAL] (the scheduler tick), free guilds
 * once per [FREE_INTERVAL] and at startup.
 * Each pod only sweeps the guilds of its own shards.
 */
@Component
class TemporaryVCCleaner(
    private val shardManager: ShardManager,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    private companion object {
        val PREMIUM_INTERVAL = 30.minutes
        val FREE_INTERVAL = 24.hours
        val STARTUP_MAX_DELAY = 5.minutes
        val PACE_BETWEEN_GUILDS = 100.milliseconds
        val PACE_BETWEEN_DELETES = 1.seconds
        const val MAX_VCS_PER_GUILD_SWEEP = 5
    }

    // IO because the repositories are blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lastSweeps = ConcurrentHashMap<Long, Long>()

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    /**
     * Covers events missed while the pod was down.
     * Delayed randomly so that all the guilds of a pod don't sweep at the same moment.
     */
    @EventListener
    fun onGuildReady(event: GuildReadyEvent) {
        val guild = event.guild

        scope.launch {
            delay(Random.nextLong(STARTUP_MAX_DELAY.inWholeMilliseconds).milliseconds)
            sweepSafely(guild)
        }
    }

    @Scheduled(
        initialDelay = 30,
        fixedDelay = 30,
        timeUnit = TimeUnit.MINUTES,
    )
    fun sweepAll() {
        scope.launch {
            shardManager.guilds.forEach { guild ->
                if (sweepSafely(guild)) {
                    delay(PACE_BETWEEN_GUILDS)
                }
            }
        }
    }

    /**
     * @return whether the guild had temporary VCs to look at
     */
    private suspend fun sweepSafely(guild: Guild): Boolean =
        try {
            sweep(guild)
        } catch (e: Exception) {
            log.error(e) { "Failed sweeping leftover temporary VCs of guild ${guild.id}" }
            false
        }

    private suspend fun sweep(guild: Guild): Boolean {
        val entities = temporaryVCRepository.getOfGuild(guild.id)
        if (entities.isEmpty()) {
            return false
        }

        val guildSettings = guildSettingsRepository.findByGuildID(guild.id)
        val now = System.currentTimeMillis()
        val interval = if (guildSettings != null && premiumManager.isGuildPremium(guildSettings)) PREMIUM_INTERVAL else FREE_INTERVAL
        val lastSweep = lastSweeps[guild.idLong]

        if (lastSweep != null && now - lastSweep < interval.inWholeMilliseconds) {
            return true
        }

        lastSweeps[guild.idLong] = now

        val reason = L.t(guildSettings.supportedLocale, "reason.vc.deleted_cleanup")
        var deleted = 0

        for (entity in entities) {
            val vc = guild.getVoiceChannelById(entity.id)

            if (vc != null && vc.members.any { !it.user.isBot }) {
                continue
            }

            temporaryVCRepository.delete(guild.id, entity.id)

            try {
                vc?.delete()?.reason(reason)?.queue()
                entity.chatID
                    ?.let { guild.getTextChannelById(it) }
                    ?.delete()
                    ?.reason(reason)
                    ?.queue()
                entity.waitingID
                    ?.let { guild.getVoiceChannelById(it) }
                    ?.delete()
                    ?.reason(reason)
                    ?.queue()
            } catch (e: Exception) {
                log.warn(e) { "Failed deleting leftover temporary VC ${entity.id} in guild ${guild.id}" }
            }

            deleted++

            delay(PACE_BETWEEN_DELETES)

            if (deleted >= MAX_VCS_PER_GUILD_SWEEP) {
                break
            }
        }

        if (deleted > 0) {
            log.info { "Cleaned $deleted leftover temporary VCs in guild ${guild.id}" }
        }

        return true
    }
}
