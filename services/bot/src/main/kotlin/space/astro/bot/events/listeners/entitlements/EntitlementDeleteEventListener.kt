package space.astro.bot.events.listeners.entitlements

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.entitlement.EntitlementDeleteEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.shared.core.services.support.SupportBotApiService
import space.astro.shared.core.daos.GuildDao

private val logger = KotlinLogging.logger {  }

@Component
class EntitlementDeleteEventListener(
    val discordApplicationConfig: DiscordApplicationConfig,
    val guildDao: GuildDao,
    val supportBotApiService: SupportBotApiService,
    val coroutineScope: CoroutineScope
) {

    @EventListener
    fun receiveEntitlementDeleteEvent(event: EntitlementDeleteEvent) {
        coroutineScope.launch {
            supportBotApiService.removePremiumRoleFromUser(event.entitlement.userId)
        }

        when (event.entitlement.skuId) {
            discordApplicationConfig.premiumServerSkuId -> {
                val guildData = guildDao.getOrCreate(event.entitlement.guildId!!)

                val removed = guildData.entitlements.removeIf { it.id == event.entitlement.id }

                if (removed) {
                    guildDao.save(guildData)
                }
            }
            else -> logger.warn { "Received entitlement with unknown sku id\nData:${event.entitlement}" }
        }
    }
}