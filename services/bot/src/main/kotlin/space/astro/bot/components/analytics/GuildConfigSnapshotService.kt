package space.astro.bot.components.analytics

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import space.astro.bot.models.analytics.GuildConfigSnapshotEventData
import space.astro.bot.properties.BigQueryProperties
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val log = KotlinLogging.logger { }

/**
 * Publishes once a day the configuration snapshot of every guild of this instance.
 * Guilds without settings are skipped: they never configured anything, and they are already known from `GUILD_EVENT`.
 */
@Service
class GuildConfigSnapshotService(
    private val properties: BigQueryProperties,
    private val shardManager: ShardManager,
    private val guildSettingsRepository: GuildSettingsRepository,
    private val premiumManager: PremiumManager,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // fixed time instead of a delay so that restarts don't produce more than one snapshot per day
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    fun snapshot() {
        if (!properties.enabled) return

        try {
            val timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString()

            shardManager.guilds.chunked(500).forEach { guilds ->
                val settingsByGuild = guildSettingsRepository.findByGuildIDIn(guilds.map { it.id }).associateBy { it.guildID }

                guilds.forEach { guild ->
                    val settings = settingsByGuild[guild.id] ?: return@forEach

                    applicationEventPublisher.publishEvent(
                        GuildConfigSnapshotEventData(
                            guildId = guild.idLong,
                            premium = premiumManager.isGuildPremium(settings),
                            generators = settings.generators.size,
                            interfaces = settings.interfaces.size,
                            connections = settings.connections.size,
                            templates = settings.templates.size,
                            timestamp = timestamp,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to publish guild config snapshots" }
        }
    }
}
