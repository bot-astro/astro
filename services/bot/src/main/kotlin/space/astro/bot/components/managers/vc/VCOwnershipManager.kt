package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService

@Component
class VCOwnershipManager(
    private val vcNameManager: VCNameManager,
    private val vcPrivateChatManager: VCPrivateChatManager,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
    private val premiumRequirementDetector: PremiumRequirementDetector,
    private val configurationErrorService: ConfigurationErrorService
) {
    /**
     * Change the owner of a temporary vc.
     * Updates the vc name properly
     *
     * **This doesn't handle owner roles!**
     * For that see [handleOwnerRoleMigration]
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
                try {
                    vcNameManager.performVCNameRefresh(this)
                } catch (_: VcOperationException) {}
                vcPrivateChatManager.performPrivateChatNameRefresh(this)
                vcWaitingRoomManager.performWaitingRoomNameRefresh(this)
            }
        }
    }

    /**
     * Migrates the owner role from an [oldOwner] to a [newOwner]
     *
     * @throws ConfigurationException
     */
    fun handleOwnerRoleMigration(
        vcOperationCTX: VCOperationCTX,
        oldOwner: Member?,
        newOwner: Member
    ) {
        vcOperationCTX.generatorData.ownerRole?.let { ownerRoleId ->
            vcOperationCTX.guild.getRoleById(ownerRoleId)?.let { ownerRole ->
                if (premiumRequirementDetector.canAssignTemporaryVCOwnerRole(vcOperationCTX.guildData)) {
                    if (oldOwner != null && oldOwner.roles.any { it.id == ownerRole.id }) {
                        vcOperationCTX.guild.removeRoleFromMember(oldOwner.user, ownerRole).queue()
                    }
                    vcOperationCTX.guild.addRoleToMember(newOwner.user, ownerRole).queue()
                } else {
                    throw ConfigurationException(configurationErrorService.premiumRequiredForOwnerRole())
                }
            }
        }
    }
}