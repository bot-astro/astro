package space.astro.bot.managers.vc

import net.dv8tion.jda.api.entities.Member
import space.astro.bot.extentions.modifyPermissionOverride
import space.astro.bot.managers.util.PermissionSets
import space.astro.bot.managers.vc.VCNameManager.performVCNameRefresh
import space.astro.bot.managers.vc.VCPrivateChatManager.performPrivateChatNameRefresh
import space.astro.bot.managers.vc.VCWaitingRoomManager.performWaitingRoomNameRefresh
import space.astro.bot.managers.vc.ctx.VCOperationCTX

object VCOwnershipManager {
    /**
     * Change the owner of a temporary vc.
     * Updates the vc name properly
     *
     * **This doesn't handle owner roles!**
     */
    fun VCOperationCTX.changeOwner(newOwner: Member) {
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
            performVCNameRefresh()
            performPrivateChatNameRefresh()
            performWaitingRoomNameRefresh()
        }
    }
}