package space.astro.bot.events.listeners.entitlements

import mu.KotlinLogging
import net.dv8tion.jda.api.events.entitlement.EntitlementCreateEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.shared.core.models.database.GuildEntitlement
import space.astro.shared.core.services.dao.GuildDao

private val logger = KotlinLogging.logger {  }

@Component
class EntitlementCreateEventListener(
    val discordApplicationConfig: DiscordApplicationConfig,
    val guildDao: GuildDao
) {
    @EventListener
    fun receiveEntitlementCreateEvent(event: EntitlementCreateEvent) {
        when (event.entitlement.skuId) {
            discordApplicationConfig.premiumServerSkuId -> {
                val guildData = guildDao.getOrCreate(event.entitlement.guildId!!)

                val entitlementIndex = guildData.entitlements.indexOfFirst { it.id == event.entitlement.id }
                if (entitlementIndex >= 0) {
                    guildData.entitlements[entitlementIndex] = GuildEntitlement(
                        event.entitlement.id,
                        event.entitlement.skuId,
                        event.entitlement.endsAt?.toInstant()?.toEpochMilli()
                    )

                    guildDao.save(guildData)
                } else {
                    guildData.entitlements.add(GuildEntitlement(
                        event.entitlement.id,
                        event.entitlement.skuId,
                        event.entitlement.endsAt?.toInstant()?.toEpochMilli()
                    ))

                    guildDao.save(guildData)
                }

                // TODO: Send event to microservice
            }
            else -> logger.warn { "Received entitlement with unknown sku id\nData:${event.entitlement}" }
        }
    }
}