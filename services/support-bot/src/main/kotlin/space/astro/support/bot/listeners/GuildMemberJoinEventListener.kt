package space.astro.support.bot.listeners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.shared.core.services.chargebee.ChargebeeClientService
import space.astro.shared.core.services.discord.DiscordEntitlementsFetchService
import space.astro.support.bot.config.DiscordApplicationConfig

private val log = KotlinLogging.logger {  }

@Component
class GuildMemberJoinEventListener(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val discordEntitlementsFetchService: DiscordEntitlementsFetchService,
    private val applicationScope: CoroutineScope,
    private val chargebeeClientService: ChargebeeClientService
) {

    @EventListener
    fun receiveGuildMemberJoinEvent(event: GuildMemberJoinEvent) {
        if (event.guild.idLong != discordApplicationConfig.guildIdForPremiumRole)
            return

        applicationScope.launch {
            var premium = false

            val subscriptions = chargebeeClientService.getActiveServerSubscriptionsOfUser(event.user.id)

            if (subscriptions.isNotEmpty()) {
                premium = true
            } else {
                val entitlements = discordEntitlementsFetchService.fetchEntitlements(
                    applicationId = discordApplicationConfig.entitlementsBotId.toString(),
                    authToken = discordApplicationConfig.entitlementsBotToken,
                    userId = event.user.id,
                    guildId = null
                )

                if (entitlements.any { it.skuId == discordApplicationConfig.premiumSkuId.toString() }) {
                    premium = true
                }
            }


             if (premium) {
                val role = event.guild.getRoleById(discordApplicationConfig.premiumRoleId)
                    ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

                event.guild.addRoleToMember(event.user, role).queue()

                log.info { "Member with id ${event.member.id} got premium role upon joining the server" }
            }
        }
    }
}