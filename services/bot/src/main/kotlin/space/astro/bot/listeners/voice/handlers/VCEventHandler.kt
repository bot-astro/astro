package space.astro.bot.listeners.voice.handlers

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import space.astro.bot.managers.cooldown.CooldownsManager
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.GuildErrorNotifier
import space.astro.bot.managers.util.PremiumRequirementDetector
import space.astro.bot.managers.vc.VCEvent
import space.astro.shared.core.services.dao.TemporaryVCDao

@Component
class VCEventHandler(
    val premiumRequirementDetector: PremiumRequirementDetector,
    val cooldownsManager: CooldownsManager,
    val temporaryVCDao: TemporaryVCDao
) {
    fun handleEvents(
        events: List<VCEvent>,
        guildErrorNotifier: GuildErrorNotifier,
        memberRolesManager: SimpleMemberRolesManager
    ) {
        runBlocking {
            events.forEach {
                when (it) {
                    is VCEvent.JoinedGenerator -> handleJoinedGeneratorEvent(it, guildErrorNotifier, memberRolesManager)
                }
            }
        }
    }
}