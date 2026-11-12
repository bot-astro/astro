package space.astro.entitlements.expiration.job

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import space.astro.entitlements.expiration.job.properties.DiscordApplicationProperties
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.clients.SupportBotApiClient
import space.astro.shared.core.exceptions.ANotFoundException
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
    private val supportBotApiClient: SupportBotApiClient,
) : ApplicationRunner {
    companion object {
        // the job runs once a day
        private const val NEXT_RUN_IN_DAYS = 1L
    }

    override fun run(args: ApplicationArguments) {
        val expirationThreshold = OffsetDateTime.now(ZoneOffset.UTC).plus(NEXT_RUN_IN_DAYS, ChronoUnit.DAYS)

        val entitlements =
            discordApiClient.getEntitlements(
                applicationId = discordApplicationProperties.id.toString(),
                botToken = discordApplicationProperties.token,
            )
        val expiredEntitlements = entitlements.filter { it.endsAt?.isBefore(expirationThreshold) == true }
        log.info { "Found ${expiredEntitlements.size} expired entitlements" }

        val expiredEntitlementIdsByGuild =
            expiredEntitlements
                .mapNotNull { entitlement -> entitlement.guildId?.let { guildId -> guildId to entitlement.id } }
                .groupBy({ it.first }, { it.second })

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

        expiredEntitlements
            .mapNotNull { it.userId }
            .distinct()
            .forEach { userId ->
                try {
                    supportBotApiClient.removePremiumRole(userId)
                } catch (_: ANotFoundException) {
                    // not in the support server
                } catch (e: Exception) {
                    failures++
                    log.error(e) { "Failed to remove the premium role of user $userId" }
                }
            }

        // makes the job fail, so that Kubernetes retries it
        check(failures == 0) { "Failed to remove the expired entitlements from $failures guilds" }
    }
}
