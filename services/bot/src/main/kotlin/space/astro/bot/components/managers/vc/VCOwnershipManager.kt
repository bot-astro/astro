package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService

@Component
class VCOwnershipManager(
    val vcNameManager: VCNameManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager,
    val configurationErrorService: ConfigurationErrorService
) {
    /**
     * Change the owner of a temporary vc.
     * Updates the vc name properly
     *
     * **This doesn't handle owner roles!**
     *
     * @throws ConfigurationException
     * @throws InsufficientPermissionException
     */
    fun changeOwner(
        vcOperationCTX: VCOperationCTX,
        newOwner: Member
    ) {
        vcOperationCTX.apply {
            /////////////////////////////
            /// OLD OWNER PERMISSIONS ///
            /////////////////////////////
            temporaryVCData.ownerId.toLong().also {
                temporaryVCManager.removePermissionOverride(it)
                privateChatManager?.removePermissionOverride(it)
                waitingRoomManager?.removePermissionOverride(it)
            }

            markTemporaryVCManagerAsUpdated()
            markPrivateChatManagerAsUpdated()
            markWaitingRoomManagerAsUpdated()


            /////////////////////////////
            /// NEW OWNER PERMISSIONS ///
            /////////////////////////////
            val ownerPermissions = generatorData.ownerPermissions.takeIf { it != 0L }
                ?: PermissionSets.ownerVCPermissions

            temporaryVCManager.modifyPermissionOverride(
                permissionHolder = newOwner,
                allow = ownerPermissions
            )

            ///////////////////////
            /// UPDATE CTX DATA ///
            ///////////////////////
            temporaryVCOwner = newOwner
            temporaryVCData.ownerId = newOwner.id
            temporaryVCData.renamed = false


            ////////////////////////////
            /// UPDATE CHANNEL NAMES ///
            ////////////////////////////
            if (generatorData.renameConditions.ownerChange) {
                vcNameManager.performVCNameRefresh(this)
                vcPrivateChatManager.performPrivateChatNameRefresh(this)
                vcWaitingRoomManager.performWaitingRoomNameRefresh(this)
            }
        }
    }
}