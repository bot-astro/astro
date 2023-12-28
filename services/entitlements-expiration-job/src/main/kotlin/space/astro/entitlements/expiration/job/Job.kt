package space.astro.entitlements.expiration.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import space.astro.entitlements.expiration.job.config.DiscordApplicationConfig
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.services.discord.DiscordEntitlementsFetchService
import java.time.OffsetDateTime
import java.time.ZoneOffset

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.entitlements.expiration.job",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.coroutine",
        "space.astro.shared.core.services.discord",
        "space.astro.shared.core.daos"
    ]
)
class Application(
    val entitlementsFetchService: DiscordEntitlementsFetchService,
    val discordApplicationConfig: DiscordApplicationConfig,
    val applicationScope: CoroutineScope,
    val guildDao: GuildDao
) {
    @EventListener
    fun applicationStarted(event: ContextRefreshedEvent) {
        applicationScope.launch {
            val expiredEntitlements = entitlementsFetchService.fetchEntitlements(
                applicationId = discordApplicationConfig.applicationId.toString(),
                authToken = discordApplicationConfig.token,
                userId = null,
                guildId = null
            ).filter {
                it.endsAt?.isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)) == true
            }

            expiredEntitlements
                .filter { it.guildId != null }
                .groupBy { it.guildId!! }
                .mapValues { it.value.map { entitlement -> entitlement.id } }
                .forEach { guildIdToEntitlementIds ->
                    val guildData = guildDao.get(guildIdToEntitlementIds.key) ?: return@forEach

                    val updated = guildData.entitlements.removeIf { savedEntitlement -> savedEntitlement.id in guildIdToEntitlementIds.value }

                    if (updated) {
                        guildDao.save(guildData)
                    }
                }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}