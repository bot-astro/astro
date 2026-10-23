package space.astro.bot.components.managers.vc

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
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
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings.InitialPosition
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings.PermissionsInherited
import java.util.Locale

@Component
class VCWaitingRoomManager {
    /**
     * Remember to update the [TemporaryVCEntity] waiting room info
     *
     * @throws InsufficientPermissionException
     * @throws ConfigurationException
     */
    suspend fun create(
        owner: Member,
        generatorSettings: GeneratorSettings,
        temporaryVC: VoiceChannel,
        temporaryVCIncrementalPosition: Int?,
        locale: Locale,
    ): VoiceChannel {
        try {
            val guild = owner.guild

            val bitrate =
                generatorSettings.waitingBitrate
                    ?.takeIf { it != 0 }
                    ?.coerceAtMost(guild.maxBitrate)
                    ?: 64000

            val name =
                VariablesManager.computeWaitingRoomName(
                    template = generatorSettings.defaultWaitingName,
                    locale = locale,
                    owner = owner,
                    temporaryVC = temporaryVC,
                    incrementalPosition = temporaryVCIncrementalPosition,
                )

            val position =
                when (generatorSettings.waitingPosition) {
                    InitialPosition.BOTTOM -> null
                    InitialPosition.BEFORE -> temporaryVC.positionRaw - VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
                    InitialPosition.AFTER -> temporaryVC.positionRaw + VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
                }?.coerceAtLeast(0)

            val category = generatorSettings.waitingCategory?.let { guild.getCategoryById(it) }

            val builder =
                guild
                    .createVoiceChannel(name)
                    .setBitrate(bitrate)
                    .setUserlimit(generatorSettings.waitingUserLimit ?: 0)
                    .setSlowmode(generatorSettings.chatSlowmode ?: 0)
                    .setPosition(position)
                    .apply { category?.let { setParent(it) } }

            // add bot permissions
            builder.addMemberPermissionOverride(
                guild.selfMember.idLong,
                PermissionSets.astroVCPermissions,
                0L,
            )

            // inherit permissions if needed
            val permissionOverrides =
                when (generatorSettings.waitingPermissionsInherited) {
                    PermissionsInherited.NONE -> emptyList()

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

            // temporary vc owner permissions
            builder.modifyPermissionOverride(
                permissionOverrides.firstOrNull { it.id == owner.id },
                owner,
                PermissionSets.ownerWaitingRoomVCPermissions,
                0,
            )

            temporaryVC.members.filter { it.id != owner.id }.forEach { vcMember ->
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
                            "\nEncountered in: creating the waiting room of a temporary VC"

                    else ->
                        "An unknown error occurred!" +
                            "\nEncountered in: creating the waiting room of a temporary VC: ${e.meaning}"
                }

            throw ConfigurationException(ConfigurationErrorEntity(guildId = owner.guild.id, description = description))
        }
    }

    /**
     * Remember to save the [TemporaryVCEntity] and queue the managers, rename counters of the entity get updated
     */
    fun performWaitingRoomNameRefresh(ctx: VCOperationCTX) {
        val waitingRoom = ctx.waitingRoom
        val waitingRoomManager = ctx.waitingRoomManager
        val entity = ctx.temporaryVCEntity

        if (waitingRoom == null || waitingRoomManager == null || !entity.canBeRenamed()) {
            return
        }

        val newName =
            VariablesManager.computeWaitingRoomName(
                template = ctx.generatorSettings.defaultWaitingName,
                locale = ctx.guildSettings.supportedLocale,
                owner = ctx.temporaryVCOwner,
                temporaryVC = ctx.temporaryVC,
                incrementalPosition = entity.incrementalPosition,
            )

        if (waitingRoom.name != newName) {
            entity.registerRename()
            waitingRoomManager.setName(newName)
            ctx.markWaitingRoomManagerAsUpdated()
        }
    }

    private fun TemporaryVCEntity.canBeRenamed(): Boolean {
        val lastChange = lastWaitingNameChange ?: return true

        return System.currentTimeMillis() - lastChange > RATE_LIMIT_WINDOW_MS || waitingNameChanges < MAX_NAME_CHANGES
    }

    private fun TemporaryVCEntity.registerRename() {
        val now = System.currentTimeMillis()
        val lastChange = lastWaitingNameChange

        waitingNameChanges = if (lastChange == null || now - lastChange > RATE_LIMIT_WINDOW_MS) 1 else waitingNameChanges + 1
        lastWaitingNameChange = now
    }

    private companion object {
        const val RATE_LIMIT_WINDOW_MS = 600_000L
        const val MAX_NAME_CHANGES = 2
    }
}
