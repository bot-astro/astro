package space.astro.bot.listeners.voice.handlers

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import space.astro.bot.managers.cooldown.CooldownsManager
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.GuildErrorNotifier
import space.astro.bot.managers.util.PremiumRequirementDetector
import space.astro.bot.managers.vc.*
import space.astro.bot.managers.vc.events.VCEvent
import space.astro.shared.core.services.dao.TemporaryVCDao

@Component
class VCEventHandler(
    val premiumRequirementDetector: PremiumRequirementDetector,
    val cooldownsManager: CooldownsManager,
    val temporaryVCDao: TemporaryVCDao,
    val guildErrorNotifier: GuildErrorNotifier,
    val vcOwnershipManager: VCOwnershipManager,
    val vcNameManager: VCNameManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager,
    val vcPositionManager: VCPositionManager
) {
    fun handleEvents(
            events: List<VCEvent>,
            memberRolesManager: SimpleMemberRolesManager
    ) {
        runBlocking {
            events.forEach {
                when (it) {
                    is VCEvent.JoinedGenerator -> handleJoinedGeneratorEvent(it, memberRolesManager)
                    is VCEvent.JoinedTemporaryVC -> handleJoinedTemporaryVCEvent(it, memberRolesManager)
                    is VCEvent.JoinedConnectedVC -> handleJoinedConnectedVCEvent(it, memberRolesManager)
                    is VCEvent.LeftTemporaryVC -> handleLeftTemporaryVCEvent(it, memberRolesManager)
                    is VCEvent.LeftConnectedVC -> handleLeftConnectedVCEvent(it, memberRolesManager)
                }
            }
        }
    }
}