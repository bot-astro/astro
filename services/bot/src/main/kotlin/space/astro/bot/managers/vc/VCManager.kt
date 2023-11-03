package space.astro.bot.managers.vc

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager
import space.astro.bot.extentions.modifyPermissionOverride
import space.astro.bot.managers.util.PermissionSets
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.TemporaryVCData

object VCManager {
    fun changeOwner(
        guild: Guild,
        vcManager: VoiceChannelManager,
        privateChatManager: TextChannelManager?,
        waitingRoomManager: VoiceChannelManager?,
        newOwner: Member,
        generatorData: GeneratorData,
        temporaryVCData: TemporaryVCData
    ) {
        /////////////////////////////
        /// OLD OWNER PERMISSIONS ///
        /////////////////////////////
        temporaryVCData.ownerId.toLong().also {
            vcManager.removePermissionOverride(it)
            privateChatManager?.removePermissionOverride(it)
            waitingRoomManager?.removePermissionOverride(it)
        }
        
        /////////////////////////////
        /// NEW OWNER PERMISSIONS ///
        /////////////////////////////
        val ownerPermissions = generatorData.ownerPermissions.takeIf { it != 0L }
                ?: PermissionSets.ownerVCPermissions
        
        vcManager.modifyPermissionOverride(
            permissionHolder = newOwner,
            allow = ownerPermissions
        )
        
        
        /////////////////////////
        /// UPDATE CACHE DATA ///
        /////////////////////////
        temporaryVCData.ownerId = newOwner.id
        temporaryVCData.renamed = false
        
        
        ////////////////////////////
        /// UPDATE CHANNEL NAMES ///
        ////////////////////////////
        if (generatorData.renameConditions.ownerChange) {
            
        }
        
        
    }
}