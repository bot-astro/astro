package space.astro.bot.managers.vc.dto

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.managers.Manager
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.TemporaryVCData

/**
 * 
 */
data class VCOperationCTX(
    val generator: VoiceChannel,
    val generatorData: GeneratorData,
    var temporaryVCOwner: Member,
    val temporaryVC: VoiceChannel,
    val temporaryVCManager: VoiceChannelManager,
    val temporaryVCData: TemporaryVCData,
    val temporaryVCsData: List<TemporaryVCData>,
    val privateChat: TextChannel?,
    val privateChatManager: TextChannelManager?,
    val waitingRoom: VoiceChannel?,
    val waitingRoomManager: VoiceChannelManager?
) {
    private var temporaryVCManagerUpdated = false
    private var privateChatManagerUpdated = false
    private var waitingRoomManagerUpdated = false

    fun markTemporaryVCManagerAsUpdated() {
        temporaryVCManagerUpdated = true
    }

    fun markPrivateChatManagerAsUpdated() {
        privateChatManagerUpdated = true
    }

    fun markWaitingRoomManagerAsUpdated() {
        waitingRoomManagerUpdated = true
    }

    fun queueUpdatedManagers(
        success: ((managerType: ManagerType) -> Unit)? = null,
        failure: ((managerType: ManagerType, throwable: Throwable) -> Unit)? = null,
    ) {
        if (temporaryVCManagerUpdated) {
            temporaryVCManager.queue(
                {
                    success?.invoke(ManagerType.TEMPORARY_VC)
                },
                { throwable ->
                    failure?.invoke(ManagerType.TEMPORARY_VC, throwable)
                }
            )
        }

        if (privateChatManagerUpdated) {
            privateChatManager?.queue(
                {
                    success?.invoke(ManagerType.PRIVATE_CHAT)
                },
                { throwable ->
                    failure?.invoke(ManagerType.PRIVATE_CHAT, throwable)
                }
            )
        }

        if (waitingRoomManagerUpdated) {
            waitingRoomManager?.queue(
                {
                    success?.invoke(ManagerType.WAITING_ROOM)
                },
                { throwable ->
                    failure?.invoke(ManagerType.WAITING_ROOM, throwable)
                }
            )
        }
    }

    enum class ManagerType {
        TEMPORARY_VC, PRIVATE_CHAT, WAITING_ROOM
    }
}