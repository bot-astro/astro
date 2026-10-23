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
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.utils.extensions.modifyPermissionOverride
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings.PermissionsInherited
import java.util.Locale

@Component
class VCPrivateChatManager {
    /**
     * Remember to update the [TemporaryVCEntity] chat info
     *
     * @throws InsufficientPermissionException
     * @throws ConfigurationException
     */
    suspend fun create(
        owner: Member,
        generatorSettings: GeneratorSettings,
        temporaryVC: VoiceChannel,
        locale: Locale,
    ): TextChannel {
        try {
            val guild = owner.guild

            val name = VariablesManager.computePrivateChatName(generatorSettings.defaultChatName, locale, owner, temporaryVC)
            val category = generatorSettings.chatCategory?.let { guild.getCategoryById(it) }

            val builder =
                guild
                    .createTextChannel(name)
                    .setSlowmode(generatorSettings.chatSlowmode ?: 0)
                    .setNSFW(generatorSettings.chatNsfw)
                    .apply {
                        generatorSettings.chatTopic?.let { setTopic(it) }
                        category?.let { setParent(it) }
                    }

            // add bot permissions
            builder.addMemberPermissionOverride(
                guild.selfMember.idLong,
                PermissionSets.astroPrivateChatPermissions,
                0L,
            )

            // inherit permissions if needed
            val permissionOverrides =
                when (generatorSettings.chatPermissionsInherited) {
                    PermissionsInherited.NONE -> {
                        // when inheriting is not enabled just deny the public role `VIEW CHANNEL` permission
                        builder.addRolePermissionOverride(
                            guild.publicRole.idLong,
                            0,
                            Permission.VIEW_CHANNEL.rawValue,
                        )
                        emptyList()
                    }

                    PermissionsInherited.CATEGORY -> {
                        builder.syncPermissionOverrides()
                        category?.permissionOverrides ?: emptyList()
                    }

                    PermissionsInherited.GENERATOR -> {
                        // copy permission overrides one by one from the generator if the perm holder is cached
                        guild.getVoiceChannelById(generatorSettings.id)?.let {
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
            generatorSettings.permissionsImmuneRole
                ?.let { guild.getRoleById(it) }
                ?.let { immuneRole ->
                    builder.modifyPermissionOverride(
                        permissionOverrides.firstOrNull { it.id == immuneRole.id },
                        immuneRole,
                        PermissionSets.immuneRolePrivateChatPermissions,
                        0,
                    )
                }

            temporaryVC.members.forEach { vcMember ->
                builder.modifyPermissionOverride(
                    permissionOverrides.firstOrNull { it.id == vcMember.id },
                    vcMember,
                    Permission.VIEW_CHANNEL.rawValue,
                    0,
                )
            }

            return builder.await()
        } catch (e: ErrorResponseException) {
            val description =
                when (e.errorResponse) {
                    ErrorResponse.MAX_CHANNELS ->
                        "Your server has reached the maximum amount of channels" +
                            "\nEncountered in: creating the private chat of a temporary VC"

                    else ->
                        "An unknown error occurred!" +
                            "\nEncountered in: creating the private chat of a temporary VC: ${e.meaning}"
                }

            throw ConfigurationException(ConfigurationErrorEntity(guildId = owner.guild.id, description = description))
        }
    }

    /**
     * Remember to save the [TemporaryVCEntity] and queue the managers, rename counters of the entity get updated
     */
    fun performPrivateChatNameRefresh(ctx: VCOperationCTX) {
        val privateChat = ctx.privateChat
        val privateChatManager = ctx.privateChatManager
        val entity = ctx.temporaryVCEntity

        if (privateChat == null || privateChatManager == null || !entity.canBeRenamed()) {
            return
        }

        val newName =
            VariablesManager.computePrivateChatName(
                template = ctx.generatorSettings.defaultChatName,
                locale = ctx.guildSettings.supportedLocale,
                owner = ctx.temporaryVCOwner,
                temporaryVC = ctx.temporaryVC,
            )

        if (privateChat.name != newName) {
            entity.registerRename()
            privateChatManager.setName(newName)
            ctx.markPrivateChatManagerAsUpdated()
        }
    }

    private fun TemporaryVCEntity.canBeRenamed(): Boolean {
        val lastChange = lastChatNameChange ?: return true

        return System.currentTimeMillis() - lastChange > RATE_LIMIT_WINDOW_MS || chatNameChanges < MAX_NAME_CHANGES
    }

    private fun TemporaryVCEntity.registerRename() {
        val now = System.currentTimeMillis()
        val lastChange = lastChatNameChange

        chatNameChanges = if (lastChange == null || now - lastChange > RATE_LIMIT_WINDOW_MS) 1 else chatNameChanges + 1
        lastChatNameChange = now
    }

    private companion object {
        const val RATE_LIMIT_WINDOW_MS = 600_000L
        const val MAX_NAME_CHANGES = 2
    }
}
