package space.astro.bot.managers.vc

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import org.springframework.stereotype.Component
import space.astro.bot.extentions.modifyPermissionOverride
import space.astro.bot.managers.util.PermissionSets
import space.astro.bot.managers.vc.ctx.VCOperationCTX
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.TemporaryVCData

@Component
class VCPrivateChatManager {
    suspend fun create(
        owner: Member,
        generatorData: GeneratorData,
        temporaryVC: VoiceChannel,
    ) : TextChannel? {
        try {
            val guild = owner.guild

            val name = VariablesManager.computePrivateChatName(generatorData.defaultChatName, owner, temporaryVC)
            val category = generatorData.chatCategory?.let { guild.getCategoryById(it) }

            val builder = guild.createTextChannel(name)
                .setSlowmode(generatorData.chatSlowmode)
                .setNSFW(generatorData.chatNsfw)
                .apply {
                    if (generatorData.chatTopic != null)
                        setTopic(generatorData.chatTopic)

                    if (category != null)
                        setParent(category)
                }

            // add bot permissions
            builder.addMemberPermissionOverride(
                guild.selfMember.idLong,
                PermissionSets.astroPrivateChatPermissions,
                0L
            )

            // inherit permissions if needed
            val permissionOverrides = when (generatorData.chatPermissionsInherited) {
                PermissionsInherited.NONE -> {
                    // when inheriting is not enabled just deny the public role `VIEW CHANNEL` permission
                    builder.addRolePermissionOverride(
                        guild.publicRole.idLong,
                        0,
                        Permission.VIEW_CHANNEL.rawValue
                    )
                    emptyList()
                }

                PermissionsInherited.CATEGORY -> {
                    builder.syncPermissionOverrides()
                    category?.permissionOverrides ?: emptyList()
                }

                PermissionsInherited.GENERATOR -> {
                    // copy permission overrides one by one from the generator if the perm holder is cached
                    guild.getVoiceChannelById(generatorData.id)?.let {
                        it.permissionOverrides.forEach { perm ->
                            perm.permissionHolder?.also { permHolder ->
                                builder.addPermissionOverride(permHolder, perm.allowedRaw, perm.deniedRaw)
                            }
                        }

                        it.permissionOverrides
                    } ?: emptyList()
                }
            }


            // immune role permissions
            generatorData.permissionsImmuneRole
                ?.let { guild.getRoleById(it) }
                ?.let {  immuneRole ->
                    builder.modifyPermissionOverride(
                        permissionOverrides.firstOrNull { it.id == immuneRole.id },
                        immuneRole,
                        PermissionSets.immuneRolePrivateChatPermissions,
                        0
                    )
                }

            temporaryVC.members.forEach { vcMember ->
                builder.modifyPermissionOverride(
                    permissionOverrides.firstOrNull { it.id == vcMember.id },
                    vcMember,
                    Permission.VIEW_CHANNEL.rawValue,
                    0
                )
            }

            return builder.await()
        } catch (e: Exception) {
            TODO("Docuement error")
            throw e
        }
    }

    fun performPrivateChatNameRefresh(
        vcOperationCTX: VCOperationCTX
    ) {
        vcOperationCTX.apply {
            if (privateChat != null
                && privateChatManager != null
                && temporaryVCData.canBeRenamed()
            ) {
                val newName = VariablesManager.computePrivateChatName(
                    template = generatorData.defaultChatName,
                    owner = temporaryVCOwner,
                    temporaryVC = temporaryVC
                )

                if (privateChat.name != newName) {
                    temporaryVCData.performRenameOperationsOnTemporaryVCData()
                    privateChatManager.setName(newName)
                    markPrivateChatManagerAsUpdated()
                }
            }
        }
    }

    /////////////////////////////////
    /// TEMPORARY VC DATA HELPERS ///
    /////////////////////////////////

    private fun TemporaryVCData.canBeRenamed(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (lastChatNameChange == null || currentTime - lastChatNameChange!! > 600000)
            return true

        if (chatNameChanges < 2)
            return true

        return false
    }

    private fun TemporaryVCData.performRenameOperationsOnTemporaryVCData() {
        if (canBeRenamed()) {
            val currentTime = System.currentTimeMillis()

            if (lastChatNameChange == null || currentTime - lastChatNameChange!! > 600000) {
                lastChatNameChange = currentTime
                chatNameChanges = 1
            } else {
                lastChatNameChange = currentTime
                chatNameChanges++
            }
        }
    }
}