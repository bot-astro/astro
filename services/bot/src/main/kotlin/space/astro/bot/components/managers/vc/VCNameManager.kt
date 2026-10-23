package space.astro.bot.components.managers.vc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.interactions.InteractionException
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.models.discord.vc.VCOperationCTX.VCOperationOrigin
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.models.database.TemporaryVCEntity.TemporaryVCState
import space.astro.shared.core.models.database.guildSettings.GeneratorSettings
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class VCNameManager(
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcPositionManager: VCPositionManager,
) {
    /**
     * Renames a temporary vc, the change is applied once [VCOperationCTX.queueUpdatedManagers] is called
     * and the temporary vc entity gets saved
     *
     * @throws InteractionException if the vc cannot be renamed right now
     * @throws ConfigurationException if the resulting name is not valid
     */
    suspend fun performVCRename(
        ctx: VCOperationCTX,
        newNameTemplate: String,
    ) {
        if (!rename(ctx, newNameTemplate)) {
            return
        }

        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guild.id, ctx.temporaryVCEntity) }
    }

    /**
     * Refreshes the name of a temporary vc based on its state, following the rename conditions of the generator
     *
     * **The temporary vc entity must be saved by the caller**, the change is applied once [VCOperationCTX.queueUpdatedManagers] is called
     *
     * @throws InteractionException if the vc cannot be renamed right now
     * @throws ConfigurationException if the resulting name is not valid
     */
    fun performVCNameRefresh(ctx: VCOperationCTX) {
        val nameTemplate = getNameTemplateForRefresh(ctx)

        rename(ctx, nameTemplate)
    }

    /**
     * @return true if the entity has been modified
     */
    private fun rename(
        ctx: VCOperationCTX,
        nameTemplate: String,
    ): Boolean {
        val entity = ctx.temporaryVCEntity

        if (!entity.canBeRenamed()) {
            throw InteractionException(
                "The voice channel has already been renamed twice in the last 10 minutes." +
                    "\nDiscord allows bots to change the name of a channel a maximum of two times every 10 minutes, " +
                    "so Astro cannot rename it again yet.",
            )
        }

        if (!ctx.shouldRenameBasedOnRenameConditions()) {
            return false
        }

        performPositionUpdates(ctx, nameTemplate)

        val newName =
            VariablesManager.computeVcNameForExisting(
                template = nameTemplate,
                owner = ctx.temporaryVCOwner,
                temporaryVC = ctx.temporaryVC,
                incrementalPosition = entity.incrementalPosition,
            )

        if (ctx.temporaryVC.name == newName) {
            return true
        }

        try {
            ctx.temporaryVCManager.setName(newName)
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException(
                ConfigurationErrorEntity(
                    guildId = ctx.guild.id,
                    description = "Invalid temporary vc name: $newName",
                ),
            )
        }

        ctx.markTemporaryVCManagerAsUpdated()

        entity.registerRename(renamedByUser = ctx.vcOperationOrigin == VCOperationOrigin.USER_RENAME)

        return true
    }

    private fun getNameTemplateForRefresh(ctx: VCOperationCTX): String {
        val settings = ctx.generatorSettings

        return when (ctx.temporaryVCEntity.state) {
            TemporaryVCState.UNLOCKED, TemporaryVCState.UNHIDDEN -> settings.defaultName
            TemporaryVCState.LOCKED -> settings.defaultLockedName
            TemporaryVCState.HIDDEN -> settings.defaultHiddenName
        } ?: settings.defaultName
    }

    /**
     * Checks whether the vc should be renamed based on the rename conditions of the generator
     */
    private fun VCOperationCTX.shouldRenameBasedOnRenameConditions(): Boolean {
        val conditions = generatorSettings.renameConditions

        if (vcOperationOrigin == VCOperationOrigin.USER_RENAME) {
            return true
        }

        if (!conditions.renamed && temporaryVCEntity.renamed) {
            return false
        }

        return when (vcOperationOrigin) {
            VCOperationOrigin.ACTIVITY_CHANGE -> conditions.activityChange
            VCOperationOrigin.OWNER_CHANGE -> conditions.ownerChange
            VCOperationOrigin.STATE_CHANGE ->
                conditions.stateChange &&
                    when (temporaryVCEntity.state) {
                        TemporaryVCState.LOCKED -> generatorSettings.defaultLockedName != null
                        TemporaryVCState.HIDDEN -> generatorSettings.defaultHiddenName != null
                        else -> true
                    }

            else -> true
        }
    }

    /**
     * Updates the incremental position of the temporary vc and the raw positions of the vc and its waiting room,
     * only if the [nameTemplate] needs positional data
     */
    private fun performPositionUpdates(
        ctx: VCOperationCTX,
        nameTemplate: String,
    ) {
        if (!VariablesManager.doesTemplateRequireVCPositionalData(nameTemplate)) {
            return
        }

        val incrementalPosition =
            vcPositionManager.getIncrementalPosition(
                generatorId = ctx.generatorSettings.id,
                excludedVCId = null,
                temporaryVCs = ctx.temporaryVCEntities,
            )
        ctx.temporaryVCEntity.incrementalPosition = incrementalPosition

        val rawPosition =
            vcPositionManager.getRawPosition(
                incrementalPosition = incrementalPosition,
                generator = ctx.generatorSettings,
                generatorVC = ctx.generator,
            ) ?: return

        ctx.temporaryVCManager.setPosition(rawPosition)
        ctx.markTemporaryVCManagerAsUpdated()

        val waitingRoomManager = ctx.waitingRoomManager ?: return

        val waitingRoomRawPosition =
            when (ctx.generatorSettings.waitingPosition) {
                GeneratorSettings.InitialPosition.AFTER -> rawPosition + VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
                GeneratorSettings.InitialPosition.BEFORE -> rawPosition - VCPositionManager.RAW_POSITIONS_WAITING_ROOM_DISTANCE
                GeneratorSettings.InitialPosition.BOTTOM -> null
            } ?: return

        waitingRoomManager.setPosition(waitingRoomRawPosition)
        ctx.markWaitingRoomManagerAsUpdated()
    }

    private fun TemporaryVCEntity.canBeRenamed(): Boolean {
        val lastChange = lastNameChange ?: return true

        return System.currentTimeMillis() - lastChange > RATE_LIMIT_WINDOW_MS || nameChanges < MAX_NAME_CHANGES
    }

    private fun TemporaryVCEntity.registerRename(renamedByUser: Boolean) {
        val now = System.currentTimeMillis()
        val lastChange = lastNameChange

        nameChanges = if (lastChange == null || now - lastChange > RATE_LIMIT_WINDOW_MS) 1 else nameChanges + 1
        lastNameChange = now

        if (renamedByUser) {
            renamed = true
        }
    }

    private companion object {
        const val RATE_LIMIT_WINDOW_MS = 600_000L
        const val MAX_NAME_CHANGES = 2
    }
}
