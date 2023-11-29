package space.astro.bot.events.listeners.voice.handlers

import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.components.managers.vc.*
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.TemporaryVCDao

@Component
class VCEventHandler(
    val premiumRequirementDetector: PremiumRequirementDetector,
    val configurationErrorService: ConfigurationErrorService,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    val cooldownsManager: CooldownsManager,
    val temporaryVCDao: TemporaryVCDao,
    val vcOwnershipManager: VCOwnershipManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager,
    val vcPositionManager: VCPositionManager
) {
    fun handleEvents(
        events: List<VCEvent>,
        memberRolesManager: SimpleMemberRolesManager
    ) {
        runBlocking {
            val generatorEvents = events.filterIsInstance<VCEvent.JoinedGenerator>()
            val nonGeneratorEvents = events.filter { it !is VCEvent.JoinedGenerator }.toMutableList()

            generatorEvents.forEach { joinedGeneratorEvent ->
                try {
                    handleJoinedGeneratorEvent(joinedGeneratorEvent, memberRolesManager)
                } catch (e: Exception) {
                    // Remove connection events that were related to this joined generator event
                    nonGeneratorEvents.removeAll { connectionEvent ->
                        connectionEvent is VCEvent.JoinedConnectedVC
                                && connectionEvent.connectionData.id == joinedGeneratorEvent.generatorData.id
                    }

                    handleException(joinedGeneratorEvent, e)
                }
            }

            nonGeneratorEvents.forEach {
                try {
                    when (it) {
                        is VCEvent.JoinedTemporaryVC -> handleJoinedTemporaryVCEvent(it, memberRolesManager)
                        is VCEvent.JoinedConnectedVC -> handleJoinedConnectedVCEvent(it, memberRolesManager)
                        is VCEvent.LeftTemporaryVC -> handleLeftTemporaryVCEvent(it, memberRolesManager)
                        is VCEvent.LeftConnectedVC -> handleLeftConnectedVCEvent(it, memberRolesManager)
                        else -> {
                            throw RuntimeException("Handler not found for VC event: $it")
                        }
                    }
                } catch (e: Exception) {
                    handleException(it, e)
                }
            }
        }
    }

    private fun handleException(vcEvent: VCEvent, e: Exception) {
        when (e) {
            is ConfigurationException -> configurationErrorEventPublisher.publishConfigurationErrorEvent(
                guildId = vcEvent.vcEventData.guild.id,
                configurationErrorDto = e.configurationErrorDto
            )
            is InsufficientPermissionException -> configurationErrorEventPublisher.publishConfigurationErrorEvent(
                guildId = vcEvent.vcEventData.guild.id,
                configurationErrorDto = e.toConfigurationErrorDto()
            )
            else -> throw e
        }
    }
}