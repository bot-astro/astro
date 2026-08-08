package space.astro.bot.models.discord.vc

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.database.TemporaryVCData

/**
 * Context for most temporary vc related operations
 *
 * This includes a method to queue the managers: [queueUpdatedManagers]
 *
 * **You should also save the new [temporaryVCData] once finished with the operations on the temporary vc**
 *
 * @param guildData
 * @param generator The generator voice channel entity
 * @param generatorData
 * @param temporaryVCOwner
 * @param temporaryVC
 * @param temporaryVCManager
 * @param temporaryVCData
 * @param temporaryVCsData all temporary vcs related to the same guild of this temporary vc
 * @param privateChat
 * @param privateChatManager
 * @param waitingRoom
 * @param waitingRoomManager
 */
data class VCOperationCTX(
    val guildData: GuildData,
    val generator: VoiceChannel,
    val generatorData: GeneratorData,
    var temporaryVCOwner: Member?,
    val temporaryVC: VoiceChannel,
    val temporaryVCManager: VoiceChannelManager,
    val temporaryVCData: TemporaryVCData,
    val temporaryVCsData: List<TemporaryVCData>,
    val privateChat: TextChannel?,
    val privateChatManager: TextChannelManager?,
    val waitingRoom: VoiceChannel?,
    val waitingRoomManager: VoiceChannelManager?,
    val vcOperationOrigin: VCOperationOrigin
) {
    val guild = generator.guild

    private var temporaryVCManagerUpdated = false
    private var privateChatManagerUpdated = false
    private var waitingRoomManagerUpdated = false

    /**
     * Marks the [temporaryVCManager] as updated
     * Useful to decide whether it should be queued or not
     */
    fun markTemporaryVCManagerAsUpdated() {
        temporaryVCManagerUpdated = true
    }

    /**
     * Marks the [privateChatManager] as updated
     * Useful to decide whether it should be queued or not
     */
    fun markPrivateChatManagerAsUpdated() {
        privateChatManagerUpdated = true
    }

    /**
     * Marks the [waitingRoomManager] as updated
     * Useful to decide whether it should be queued or not
     */
    fun markWaitingRoomManagerAsUpdated() {
        waitingRoomManagerUpdated = true
    }


    /**
     * Queues [temporaryVCManager], [privateChatManager] and [waitingRoomManager]
     * but checks for each one if it has been updated
     *
     * @param success Success handler (pass null to use default), which receives a [ManagerType] that indicates the manager queued
     * @param failure Failure handler (pass null to use default), which receives a [ManagerType] that indicates the manager queued
     */
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

    enum class ManagerType(
        val readableName: String
    ) {
        TEMPORARY_VC("temporary vc"),
        PRIVATE_CHAT("private chat"),
        WAITING_ROOM("waiting room")
    }

    enum class VCOperationOrigin {
        USER_RENAME,
        STATE_CHANGE,
        OWNER_CHANGE,
        ACTIVITY_CHANGE,
        UNKNOWN
    }
}