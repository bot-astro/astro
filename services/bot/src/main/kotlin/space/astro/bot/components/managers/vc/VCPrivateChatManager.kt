package space.astro.bot.components.managers.vc

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.springframework.stereotype.Component
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.TemporaryVCData
import java.lang.IllegalArgumentException

@Component
class VCPrivateChatManager(
    private val configurationErrorService: ConfigurationErrorService
) {
    suspend fun create(
        owner: Member,
        generatorData: GeneratorData,
        temporaryVC: VoiceChannel,
    ) : TextChannel? {
        try {
            val guild = owner.guild

            val name = VariablesManager.computePrivateChatName(generatorData.defaultChatName, owner, temporaryVC)
            val category = generatorData.chatCategory?.let { guild.getCategoryById(it) }

            val builder = try {
                guild.createTextChannel(name)
                    .setSlowmode(generatorData.chatSlowmode)
                    .setNSFW(generatorData.chatNsfw)
                    .apply {
                        if (generatorData.chatTopic != null)
                            setTopic(generatorData.chatTopic)

                        if (category != null)
                            setParent(category)
                    }
            } catch (e: InsufficientPermissionException) {
                throw ConfigurationException(
                    configurationErrorService.missingGuildManageChannelPermission(
                        requiredFor = "creating private chat for temporary voice channels"
                    )
                )
            } catch (e: IllegalArgumentException) {
                throw ConfigurationException(
                    configurationErrorService.invalidChannelName(
                        encounteredIn = "private chat name: $name"
                    )
                )
            }

            // add bot permissions
            try {
                builder.addMemberPermissionOverride(
                    guild.selfMember.idLong,
                    PermissionSets.astroPrivateChatPermissions,
                    0L
                )
            } catch (e: InsufficientPermissionException) {
                throw ConfigurationException(
                    configurationErrorService.missingGuildManageRolesPermission(
                        requiredFor = "private chat permission handling" +
                                "\nYou can also assign Astro the following permissions in the server settings to solve this:" +
                                Permission.getPermissions(PermissionSets.astroPrivateChatPermissions).joinToString { "\n- ${it.getName()} " }
                    )
                )
            }

            // inherit permissions if needed
            val permissionOverrides = when (generatorData.chatPermissionsInherited) {
                PermissionsInherited.NONE -> {
                    // when inheriting is not enabled just deny the public role `VIEW CHANNEL` permission
                    try {
                        builder.addRolePermissionOverride(
                            guild.publicRole.idLong,
                            0,
                            Permission.VIEW_CHANNEL.rawValue
                        )
                    } catch (e: InsufficientPermissionException) {
                        throw ConfigurationException(
                            configurationErrorService.missingGuildManageRolesPermission(
                                requiredFor = "private chat permission handling" +
                                        "\nYou can also assign Astro the following permissions in the server settings to solve this:" +
                                        "\n- ${Permission.VIEW_CHANNEL.getName()} "
                            )
                        )
                    }
                    emptyList()
                }

                PermissionsInherited.CATEGORY -> {
                    try {
                        builder.syncPermissionOverrides()
                    } catch (e: IllegalArgumentException) {
                        throw ConfigurationException(
                            configurationErrorService.missingChannelParent(
                                requiredFor = "private chat is set to inherit the permissions from the category but no category was found!"
                            )
                        )
                    }

                    category?.permissionOverrides ?: emptyList()
                }

                PermissionsInherited.GENERATOR -> {
                    // copy permission overrides one by one from the generator if the perm holder is cached
                    guild.getVoiceChannelById(generatorData.id)?.let {
                        try {
                            it.permissionOverrides.forEach { perm ->
                                perm.permissionHolder?.also { permHolder ->
                                    builder.addPermissionOverride(permHolder, perm.allowedRaw, perm.deniedRaw)
                                }
                            }
                        } catch (e: InsufficientPermissionException) {
                            throw ConfigurationException(
                                configurationErrorService.missingGuildManageRolesPermission(
                                    requiredFor = "private chat permission handling" +
                                            "\nAlternative fix: private chats have been configured to inherit the permissions from the generator" +
                                            ", to do so Astro needs to have Allowed in the server settings all the permissions that are set to either Allowed or Denied in the temporary VC generator"
                                )
                            )
                        }

                        it.permissionOverrides
                    } ?: emptyList()
                }
            }


            // immune role permissions
            try {
                generatorData.permissionsImmuneRole
                    ?.let { guild.getRoleById(it) }
                    ?.let { immuneRole ->
                        builder.modifyPermissionOverride(
                            permissionOverrides.firstOrNull { it.id == immuneRole.id },
                            immuneRole,
                            PermissionSets.immuneRolePrivateChatPermissions,
                            0
                        )
                    }
            } catch (e: InsufficientPermissionException) {
                throw ConfigurationException(
                    configurationErrorService.missingGuildManageRolesPermission(
                        requiredFor = "private chat permission handling" +
                                "\nYou can also assign Astro the following permissions in the server settings to solve this:" +
                                Permission.getPermissions(PermissionSets.immuneRolePrivateChatPermissions).joinToString { "\n- ${it.getName()} " }
                    )
                )
            }

            try {
                temporaryVC.members.forEach { vcMember ->
                    builder.modifyPermissionOverride(
                        permissionOverrides.firstOrNull { it.id == vcMember.id },
                        vcMember,
                        Permission.VIEW_CHANNEL.rawValue,
                        0
                    )
                }
            } catch (e: InsufficientPermissionException) {
                throw ConfigurationException(
                    configurationErrorService.missingGuildManageRolesPermission(
                        requiredFor = "private chat permission handling" +
                                "\nYou can also assign Astro the following permissions in the server settings to solve this:" +
                                "\n- ${Permission.VIEW_CHANNEL.getName()} "
                    )
                )
            }

            return builder.await()
        } catch (e: ErrorResponseException) {
            val configError = when (e.errorResponse) {
                ErrorResponse.MAX_CHANNELS -> configurationErrorService.maximumAmountOfChannelsReached(
                    encounteredIn = "creating the private chat of a temporary VC"
                )
                ErrorResponse.MISSING_PERMISSIONS -> configurationErrorService.unknownMissingPermission(
                    requiredFor = "creating the private chat of a temporary VC"
                )
                else -> configurationErrorService.unknownError(
                    encounteredIn = "creating the private chat of a temporary VC"
                )
            }

            throw ConfigurationException(configError)
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