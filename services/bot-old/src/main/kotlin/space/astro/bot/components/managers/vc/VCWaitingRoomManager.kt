package space.astro.bot.components.managers.vc

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
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
import space.astro.shared.core.components.managers.VariablesManager
import space.astro.shared.core.models.database.GeneratorData
import space.astro.shared.core.models.database.InitialPosition
import space.astro.shared.core.models.database.PermissionsInherited
import space.astro.shared.core.models.database.TemporaryVCData

@Component
class VCWaitingRoomManager(
    private val configurationErrorService: ConfigurationErrorService
) {
    /**
     * @throws InsufficientPermissionException
     * @throws ConfigurationException
     */
    suspend fun create(
        owner: Member,
        generatorData: GeneratorData,
        temporaryVC: VoiceChannel,
        temporaryVCIncrementalPosition: Int?
    ) : VoiceChannel {
        try {
            val guild = owner.guild

            val bitrate = generatorData.waitingBitrate
                .takeIf { it != 0 }
                ?.coerceAtMost(guild.maxBitrate)
                ?: 64000

            val name = VariablesManager.computeVcNameForExisting(
                template = generatorData.defaultWaitingName,
                owner = owner,
                temporaryVC = temporaryVC,
                incrementalPosition = temporaryVCIncrementalPosition
            )

            val position = when (generatorData.waitingPosition) {
                InitialPosition.BOTTOM -> null
                InitialPosition.BEFORE -> temporaryVC.positionRaw - VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
                InitialPosition.AFTER -> temporaryVC.positionRaw + VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
            }?.coerceAtLeast(0)

            val category = generatorData.waitingCategory?.let { guild.getCategoryById(it) }

            val builder = guild.createVoiceChannel(name)
                .setBitrate(bitrate)
                .setUserlimit(generatorData.waitingUserLimit)
                .setSlowmode(generatorData.chatSlowmode)
                .setPosition(position)
                .apply {
                    if (category != null)
                        setParent(category)
                }

            // add bot permissions
            builder.addMemberPermissionOverride(
                guild.selfMember.idLong,
                PermissionSets.astroVCPermissions,
                0L
            )

            // inherit permissions if needed
            val permissionOverrides = when (generatorData.waitingPermissionsInherited) {
                PermissionsInherited.NONE -> emptyList()

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

            // temporary vc owner permissions
            builder.modifyPermissionOverride(
                permissionOverrides.firstOrNull { it.id == owner.id },
                owner,
                PermissionSets.ownerWaitingRoomVCPermissions,
                0
            )

            temporaryVC.members.filter { it.id != owner.id }.forEach { vcMember ->
                builder.modifyPermissionOverride(
                    permissionOverrides.firstOrNull { it.id == vcMember.id },
                    vcMember,
                    Permission.VIEW_CHANNEL.rawValue,
                    0
                )
            }

            return builder.await()
        } catch (e: ErrorResponseException) {
            val configError = when (e.errorResponse) {
                ErrorResponse.MAX_CHANNELS -> configurationErrorService.maximumAmountOfChannelsReached(
                    guildId = owner.guild.id,
                    encounteredIn = "creating the waiting room of a temporary VC"
                )
                else -> configurationErrorService.unknown(
                    guildId = owner.guild.id,
                    encounteredIn = "creating the waiting room of a temporary VC: ${e.meaning}"
                )
            }

            throw ConfigurationException(configError)
        }
    }

    fun performWaitingRoomNameRefresh(
        vcOperationCTX: VCOperationCTX
    ) {
        vcOperationCTX.apply {
            if (waitingRoom != null &&
                waitingRoomManager != null &&
                temporaryVCData.canBeRenamed()
            ) {
                val newName = VariablesManager.computeWaitingRoomName(
                    template = generatorData.defaultWaitingName,
                    owner = temporaryVCOwner,
                    temporaryVC = temporaryVC,
                    incrementalPosition = temporaryVCData.incrementalPosition
                )

                if (waitingRoom.name != newName) {
                    temporaryVCData.performRenameOperationsOnTemporaryVCData()
                    waitingRoomManager.setName(newName)
                    markWaitingRoomManagerAsUpdated()
                }
            }
        }
    }

    /////////////////////////////////
    /// TEMPORARY VC DATA HELPERS ///
    /////////////////////////////////

    private fun TemporaryVCData.canBeRenamed(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (lastWaitingNameChange == null || currentTime - lastWaitingNameChange!! > 600000)
            return true

        if (waitingNameChanges < 2)
            return true

        return false
    }

    private fun TemporaryVCData.performRenameOperationsOnTemporaryVCData() {
        if (canBeRenamed()) {
            val currentTime = System.currentTimeMillis()

            if (lastWaitingNameChange == null || currentTime - lastWaitingNameChange!! > 600000) {
                lastWaitingNameChange = currentTime
                waitingNameChanges = 1
            } else {
                lastWaitingNameChange = currentTime
                waitingNameChanges++
            }
        }
    }
}