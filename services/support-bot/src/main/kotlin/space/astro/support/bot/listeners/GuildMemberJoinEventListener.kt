package space.astro.support.bot.listeners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.support.bot.config.DiscordApplicationConfig
import space.astro.shared.core.services.discord.DiscordEntitlementsFetchService

@Component
class GuildMemberJoinEventListener(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val discordEntitlementsFetchService: DiscordEntitlementsFetchService,
    private val applicationScope: CoroutineScope
) {

    @EventListener
    fun receiveGuildMemberJoinEvent(event: GuildMemberJoinEvent) {
        if (event.guild.idLong != discordApplicationConfig.guildIdForPremiumRole)
            return

        applicationScope.launch {
            val entitlements = discordEntitlementsFetchService.fetchEntitlements(
                applicationId = discordApplicationConfig.entitlementsBotId.toString(),
                authToken = discordApplicationConfig.entitlementsBotToken,
                userId = event.user.id,
                guildId = null
            )

            if (entitlements.any { it.skuId == discordApplicationConfig.premiumSkuId.toString() }) {
                val role = event.guild.getRoleById(discordApplicationConfig.premiumRoleId)
                    ?: throw RuntimeException("Could not find role for premium users with id ${discordApplicationConfig.premiumRoleId}!")

                event.guild.addRoleToMember(event.user, role).queue()
            }
        }
    }
}