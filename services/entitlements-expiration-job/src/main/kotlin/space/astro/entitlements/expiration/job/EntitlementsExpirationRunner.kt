package space.astro.entitlements.expiration.job

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.entitlements.expiration.job.properties.DiscordApplicationProperties
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

/**
 * Removes from the guilds the entitlements that expired, or that will expire before the next run of the job.
 */
@Component
class EntitlementsExpirationRunner(
    private val discordApiClient: DiscordApiClient,
    private val discordApplicationProperties: DiscordApplicationProperties,
    private val guildSettingsRepository: GuildSettingsRepository,
) : ApplicationRunner {
    companion object {
        // the job runs once a day
        private const val NEXT_RUN_IN_DAYS = 1L
    }

    override fun run(args: ApplicationArguments) {
        val expirationThreshold = OffsetDateTime.now(ZoneOffset.UTC).plus(NEXT_RUN_IN_DAYS, ChronoUnit.DAYS)

        val expiredEntitlementIdsByGuild =
            discordApiClient
                .getEntitlements(
                    applicationId = discordApplicationProperties.id.toString(),
                    botToken = discordApplicationProperties.token,
                )
                .filter { it.endsAt?.isBefore(expirationThreshold) == true }
                .also { log.info { "Found ${it.size} expired entitlements" } }
                .mapNotNull { entitlement -> entitlement.guildId?.let { guildId -> guildId to entitlement.id } }
                .groupBy({ it.first }, { it.second })

        // TODO: premium, the legacy job did not remove the premium role from the user that owns the entitlement either
        var failures = 0
        expiredEntitlementIdsByGuild.forEach { (guildId, entitlementIds) ->
            try {
                val guildSettings = guildSettingsRepository.findByGuildID(guildId) ?: return@forEach

                if (guildSettings.entitlements.removeIf { it.id in entitlementIds }) {
                    guildSettingsRepository.save(guildSettings)
                    log.info { "Removed premium from guild $guildId" }
                }
            } catch (e: Exception) {
                failures++
                log.error(e) { "Failed to remove the expired entitlements from guild $guildId" }
            }
        }

        // makes the job fail, so that Kubernetes retries it
        check(failures == 0) { "Failed to remove the expired entitlements from $failures guilds" }
    }
}
