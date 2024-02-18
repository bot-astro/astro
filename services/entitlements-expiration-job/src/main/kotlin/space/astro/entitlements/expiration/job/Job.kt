package space.astro.entitlements.expiration.job

import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.context.WebApplicationContext
import space.astro.entitlements.expiration.job.config.DiscordApplicationConfig
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.services.discord.DiscordEntitlementsFetchService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {  }

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "space.astro.entitlements.expiration.job",
        "space.astro.shared.core.configs",
        "space.astro.shared.core.components.io",
        "space.astro.shared.core.services.redis",
        "space.astro.shared.core.components.kmongo",
        "space.astro.shared.core.components.influx",
        "space.astro.shared.core.services.discord",
        "space.astro.shared.core.daos"
    ]
)
class Application(
    val entitlementsFetchService: DiscordEntitlementsFetchService,
    val discordApplicationConfig: DiscordApplicationConfig,
    val guildDao: GuildDao
) {
    @EventListener
    fun applicationStarted(event: ContextRefreshedEvent) {
        runBlocking {
            try {
                val expiredEntitlements = entitlementsFetchService.fetchEntitlements(
                    applicationId = discordApplicationConfig.applicationId.toString(),
                    authToken = discordApplicationConfig.token,
                    userId = null,
                    guildId = null
                ).filter {
                    it.endsAt?.isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1)) == true
                }

                log.info { "Found expired entitlements: $expiredEntitlements" }

                expiredEntitlements
                    .filter { it.guildId != null }
                    .groupBy { it.guildId!! }
                    .mapValues { it.value.map { entitlement -> entitlement.id } }
                    .forEach { guildIdToEntitlementIds ->
                        log.info { "Removing premium from guild ${guildIdToEntitlementIds.key}" }

                        val guildData = guildDao.get(guildIdToEntitlementIds.key) ?: return@forEach

                        val updated = guildData.entitlements.removeIf { savedEntitlement -> savedEntitlement.id in guildIdToEntitlementIds.value }

                        if (updated) {
                            log.info { "Removed premium from guild ${guildData.guildID}" }
                            guildDao.save(guildData)
                        }
                    }

                log.info { "Deleted ${expiredEntitlements.size} expired entitlements" }
            } catch (e: Exception) {
                Sentry.captureException(e)
            } finally {
                (event.applicationContext as ConfigurableApplicationContext).close()
                exitProcess(0)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}