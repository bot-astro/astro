package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.utils.extensions.modifyPermissionOverride

@Component
class VCOwnershipManager(
    private val vcPrivateChatManager: VCPrivateChatManager,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
) {
    /**
     * Changes the owner of a temporary vc and refreshes the names
     *
     * The changes are applied once [VCOperationCTX.queueUpdatedManagers] is called and
     * **the temporary vc entity must be saved by the caller**
     *
     * **This doesn't handle owner roles!** For that see [handleOwnerRoleMigration]
     *
     * TODO: refresh the vc name (needs the rename conditions in `VCNameManager`)
     *
     * @throws ConfigurationException
     * @throws InsufficientPermissionException
     */
    suspend fun changeOwner(
        ctx: VCOperationCTX,
        newOwner: Member,
    ) {
        val generatorSettings = ctx.generatorSettings

        // old owner permissions
        ctx.temporaryVCEntity.ownerID.toLong().also {
            ctx.temporaryVCManager.removePermissionOverride(it)
            ctx.privateChatManager?.removePermissionOverride(it)
            ctx.waitingRoomManager?.removePermissionOverride(it)
        }

        ctx.markTemporaryVCManagerAsUpdated()
        ctx.markPrivateChatManagerAsUpdated()
        ctx.markWaitingRoomManagerAsUpdated()

        // new owner permissions
        val ownerPermissions =
            generatorSettings.ownerPermissions?.takeIf { it != 0L }
                ?: PermissionSets.ownerVCPermissions

        ctx.temporaryVCManager.modifyPermissionOverride(
            permissionHolder = newOwner,
            allow = ownerPermissions,
        )

        ctx.temporaryVCOwner = newOwner
        ctx.temporaryVCEntity.ownerID = newOwner.id
        ctx.temporaryVCEntity.renamed = false

        // channel names
        if (generatorSettings.renameConditions.ownerChange) {
            vcPrivateChatManager.performPrivateChatNameRefresh(ctx)
            vcWaitingRoomManager.performWaitingRoomNameRefresh(ctx)
        }
    }

    /**
     * Migrates the owner role of the generator from [oldOwner] to [newOwner]
     *
     * TODO: premium is not implemented yet, the legacy code required premium to assign the owner role
     */
    fun handleOwnerRoleMigration(
        ctx: VCOperationCTX,
        oldOwner: Member?,
        newOwner: Member,
    ) {
        val ownerRole =
            ctx.generatorSettings.ownerRole?.let { ctx.guild.getRoleById(it) }
                ?: return

        if (oldOwner != null && oldOwner.roles.any { it.id == ownerRole.id }) {
            ctx.guild.removeRoleFromMember(oldOwner.user, ownerRole).queue()
        }
        ctx.guild.addRoleToMember(newOwner.user, ownerRole).queue()
    }
}
