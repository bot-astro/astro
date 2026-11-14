package space.astro.bot.components.managers.vc

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
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
 * Deletes temporary VCs that were left over, for example because a voice event was missed during a restart.
 * Premium guilds are swept every 30 minutes, free guilds once a day and at startup.
 */
@Component
class TemporaryVCCleaner(
    private val shardManager: ShardManager,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
) {
    companion object {
        private val PREMIUM_INTERVAL = 30.minutes
        private val FREE_INTERVAL = 24.hours
        private val STARTUP_MAX_DELAY = 5.minutes
        private val PACE_BETWEEN_GUILDS = 100.milliseconds
        private val PACE_BETWEEN_DELETES = 1.seconds
        private const val MAX_VCS_PER_GUILD_SWEEP = 5
    }

    // IO because the repositories are blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lastSweeps = ConcurrentHashMap<Long, Long>()

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onGuildReady(event: GuildReadyEvent) {
        val guild = event.guild

        scope.launch {
            // random delay so that the guilds of a pod don't all sweep at the same moment after a restart
            delay(Random.nextLong(STARTUP_MAX_DELAY.inWholeMilliseconds).milliseconds)
            sweepSafely(guild)
        }
    }

    @Scheduled(initialDelay = 30, fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
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
        // the cache can be partial while a shard is not connected, so a channel or member that looks missing could be alive
        if (guild.jda.status != JDA.Status.CONNECTED) {
            return false
        }

        val entities = temporaryVCRepository.getOfGuild(guild.id)
        if (entities.isEmpty()) {
            return false
        }

        val now = System.currentTimeMillis()
        val lastSweep = lastSweeps[guild.idLong]

        if (lastSweep != null && now - lastSweep < PREMIUM_INTERVAL.inWholeMilliseconds) {
            return true
        }

        val guildSettings = guildSettingsRepository.findByGuildID(guild.id)
        val interval = if (guildSettings != null && premiumManager.isGuildPremium(guildSettings)) PREMIUM_INTERVAL else FREE_INTERVAL

        if (lastSweep != null && now - lastSweep < interval.inWholeMilliseconds) {
            return true
        }

        lastSweeps[guild.idLong] = now

        val reason = L.t(guildSettings.supportedLocale, "reason.vc.deleted_cleanup")
        val leftovers =
            entities
                .filter { entity ->
                    val vc = guild.getVoiceChannelById(entity.id)
                    vc == null || vc.members.none { !it.user.isBot }
                }.take(MAX_VCS_PER_GUILD_SWEEP)

        for (entity in leftovers) {
            val vc = guild.getVoiceChannelById(entity.id)

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

            delay(PACE_BETWEEN_DELETES)
        }

        if (leftovers.isNotEmpty()) {
            log.info { "Cleaned ${leftovers.size} leftover temporary VCs in guild ${guild.id}" }
        }

        return true
    }
}
