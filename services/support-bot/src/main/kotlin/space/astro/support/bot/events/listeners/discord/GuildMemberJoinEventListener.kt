package space.astro.support.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.support.bot.components.PremiumRoleService
import space.astro.support.bot.properties.SupportServerProperties

private val log = KotlinLogging.logger { }

@Component
class GuildMemberJoinEventListener(
    private val premiumRoleService: PremiumRoleService,
    private val supportServerProperties: SupportServerProperties,
) {
    // IO because checking the premium status is blocking
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @PreDestroy
    fun shutdown() {
        scope.cancel()
    }

    @EventListener
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild.id != supportServerProperties.guildId) return

        val userId = event.user.id

        scope.launch {
            try {
                if (premiumRoleService.hasPremium(userId)) {
                    premiumRoleService.addPremiumRole(userId)
                    log.info { "Member with id $userId got the premium role upon joining the server" }
                }
            } catch (e: Exception) {
                log.error(e) { "Unhandled exception while giving the premium role to the new member with id $userId" }
            }
        }
    }
}
