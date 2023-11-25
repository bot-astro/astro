package space.astro.bot.core.managers.vc

import net.dv8tion.jda.api.entities.Member
import org.springframework.stereotype.Component
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.managers.util.PermissionSets
import space.astro.bot.core.managers.vc.ctx.VCOperationCTX

@Component
class VCOwnershipManager(
    val vcNameManager: VCNameManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager
) {
    /**
     * Change the owner of a temporary vc.
     * Updates the vc name properly
     *
     * **This doesn't handle owner roles!**
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