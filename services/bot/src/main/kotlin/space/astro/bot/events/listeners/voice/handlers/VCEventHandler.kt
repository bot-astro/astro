package space.astro.bot.events.listeners.voice.handlers

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.components.managers.vc.*
import space.astro.bot.core.managers.vc.*
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.shared.core.daos.TemporaryVCDao

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